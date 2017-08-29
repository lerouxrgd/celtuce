(ns celtuce.connector
  (:require
   [clojure.java.io :as io]
   [celtuce.codec :refer [nippy-codec]]
   [celtuce.commands :as cmds])
  (:import 
   (java.util.concurrent TimeUnit)
   (com.lambdaworks.redis
    RedisClient
    ClientOptions ClientOptions$DisconnectedBehavior
    SocketOptions SslOptions)
   (com.lambdaworks.redis.codec RedisCodec)
   (com.lambdaworks.redis.api StatefulRedisConnection)
   (com.lambdaworks.redis.cluster RedisClusterClient)
   (com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection)
   (com.lambdaworks.redis.pubsub StatefulRedisPubSubConnection RedisPubSubListener)))

(defprotocol RedisConnector
  "Manipulate Redis client and stateful connection"
  (commands-sync  [this])
  (commands-async [this])
  (flush-commands [this])
  (set-options    [this options])
  (reset          [this])
  (shutdown       [this]))

(defn ^SocketOptions socket-options
  "Internal helper to build SocketOptions, used by client-options"
  [opts]
  (cond-> (SocketOptions/builder)
    (and (contains? opts :timeout)
         (contains? opts :unit))
    (.connectTimeout (:timeout opts) (:unit opts))
    (contains? opts :keep-alive)
    (.keepAlive (:keep-alive opts))
    (contains? opts :tcp-no-delay)
    (.tcpNoDelay (:tcp-no-delay opts))
    true (.build)))

(defn ^SslOptions ssl-options
  "Internal helper to build SslOptions, used by client-options"
  [opts]
  (cond-> (SslOptions/builder)
    ;; provider setup
    (contains? opts :provider)
    (cond->
      (= :open-ssl (:provider opts)) (.openSslProvider)
      (= :jdk      (:provider opts)) (.jdkSslProvider))
    ;; keystore setup
    (contains? opts :keystore)
    (cond->
      (and (contains? (:keystore opts) :file)
           (contains? (:keystore opts) :password)) 
      (.keystore (io/as-file (-> opts :keystore :file))
                 (chars      (-> opts :keystore :password)))
      (contains? (:keystore opts) :file)
      (.keystore (io/as-file (-> opts :keystore :file)))
      (and (contains? (:keystore opts) :url)
           (contains? (:keystore opts) :password)) 
      (.keystore (io/as-url (-> opts :keystore :url))
                 (chars     (-> opts :keystore :password)))
      (contains? (:keystore opts) :url)
      (.keystore (io/as-url (-> opts :keystore :url))))
    ;; truststore setup
    (contains? opts :truststore)
    (cond->
      (and (contains? (:truststore opts) :file)
           (contains? (:truststore opts) :password)) 
      (.truststore (io/as-file (-> opts :truststore :file))
                   (-> opts :truststore :password str))
      (contains? (:truststore opts) :file)
      (.truststore (io/as-file (-> opts :truststore :file)))
      (and (contains? (:truststore opts) :url)
           (contains? (:truststore opts) :password)) 
      (.truststore (io/as-url (-> opts :truststore :url))
                   ^String (-> opts :truststore :password str))
      (contains? (:truststore opts) :url)
      (.truststore (io/as-url (-> opts :truststore :url))))
    ;; finally, build
    true (.build)))

(defn ^ClientOptions client-options 
  "Builds a ClientOptions from a map of options"
  [opts]
  (cond-> (ClientOptions/builder)
    (contains? opts :ping-before-sctivate-connection)
    (.pingBeforeActivateConnection (:ping-before-sctivate-connection opts))
    (contains? opts :auto-reconnect)
    (.autoReconnect (:auto-reconnect opts))
    (contains? opts :suspend-reconnect-on-protocol-failure)
    (.suspendReconnectOnProtocolFailure (:suspend-reconnect-on-protocol-failure opts))
    (contains? opts :cancel-commands-on-reconnect-failure)
    (.cancelCommandsOnReconnectFailure (:cancel-commands-on-reconnect-failure opts))
    (contains? opts :request-queue-size)
    (.requestQueueSize (:request-queue-size opts))
    (contains? opts :disconnected-behavior)
    (.disconnectedBehavior
     (case (:disconnected-behavior opts)
       :default          ClientOptions$DisconnectedBehavior/DEFAULT
       :accept-commands  ClientOptions$DisconnectedBehavior/ACCEPT_COMMANDS
       :reject-commmands ClientOptions$DisconnectedBehavior/REJECT_COMMANDS
       (throw (ex-info "Unknown :disconnected-behavior" opts))))
    (contains? opts :socket-options)
    (.socketOptions (socket-options (:socket-options opts)))
    (contains? opts :ssl-options)
    (.sslOptions (:ssl-options opts))
    true (.build)))

;;
;; Redis Server
;;

(defrecord RedisServer 
    [^RedisClient redis-cli
     ^StatefulRedisConnection stateful-conn
     ^RedisCodec codec]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.impl.server.sync])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.impl.server.async])
    (.async stateful-conn))
  (flush-commands [this] 
    (.flushCommands stateful-conn))
  (reset [this] 
    (.reset stateful-conn))
  (shutdown [this]
    (.close stateful-conn)
    (.shutdown redis-cli)))

(defn redis-server
  [^String redis-uri & 
   {codec :codec
    cli-opts :client-options
    {auto-flush :auto-flush conn-timeout :timeout conn-unit :unit
     :or {auto-flush true conn-unit TimeUnit/MILLISECONDS}} :conn-options
    :or {codec (nippy-codec) cli-opts {}}}]
  (let [redis-cli (RedisClient/create redis-uri)
        _ (.setOptions redis-cli (client-options cli-opts))
        stateful-conn (.connect redis-cli ^RedisCodec codec)]
    (when (and conn-timeout conn-unit)
      (.setTimeout stateful-conn conn-timeout conn-unit))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (->RedisServer redis-cli stateful-conn codec)))

;;
;; Redis Cluster
;;

(defrecord RedisCluster 
    [^RedisClusterClient redis-cli
     ^StatefulRedisClusterConnection stateful-conn
     ^RedisCodec codec]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.impl.cluster.sync])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.impl.cluster.async])
    (.async stateful-conn))
  (flush-commands [this] 
    (.flushCommands stateful-conn))
  (reset [this] 
    (.reset stateful-conn))
  (shutdown [this]
    (.close stateful-conn)
    (.shutdown redis-cli)))

(defn redis-cluster
  [^String redis-uri &
   {codec :codec
    cli-opts :client-options
    {auto-flush :auto-flush conn-timeout :timeout conn-unit :unit
     :or {auto-flush true conn-unit TimeUnit/MILLISECONDS}} :conn-options
    :or {codec (nippy-codec) cli-opts {}}}]
  (let [redis-cli (RedisClusterClient/create redis-uri)
        ;; TODO call .setOptions on redis-cli
        stateful-conn (.connect redis-cli codec)]
    (when (and conn-timeout conn-unit)
      (.setTimeout stateful-conn conn-timeout conn-unit))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (->RedisCluster redis-cli stateful-conn codec)))

;;
;; Redis PubSub
;;

(defprotocol Listenable
  "Register a celtuce.commands.PubSubListener on a stateful pubsub connection"
  (add-listener! [this listener]))

(defrecord RedisPubSub 
    [redis-cli ^StatefulRedisPubSubConnection stateful-conn]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.impl.pubsub])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.impl.pubsub])
    (.async stateful-conn))
  (flush-commands [this] 
    (.flushCommands stateful-conn))
  (reset [this] 
    (.reset stateful-conn))
  (shutdown [this]
    (.close stateful-conn)
    (condp instance? redis-cli
      RedisClusterClient (.shutdown ^RedisClusterClient redis-cli)
      RedisClient        (.shutdown ^RedisClient        redis-cli)))
  Listenable
  (add-listener! [this listener]
    (.addListener
     stateful-conn
     (reify
       RedisPubSubListener
       (message [_ ch msg]
         (cmds/message listener ch msg))
       (message [_ p ch msg]
         (cmds/message listener p ch msg))
       (subscribed [_ ch cnt]
         (cmds/subscribed listener ch cnt))
       (unsubscribed [_ ch cnt]
         (cmds/unsubscribed listener ch cnt))
       (psubscribed [_ p cnt]
         (cmds/psubscribed listener p cnt))
       (punsubscribed [_ p cnt]
         (cmds/punsubscribed listener p cnt))))))

(defn ->pubsub [{:keys [redis-cli ^RedisCodec codec] :as redis-connector}]
  (->RedisPubSub
   redis-cli
   (condp instance? redis-cli
     RedisClusterClient (.connectPubSub ^RedisClusterClient redis-cli codec)
     RedisClient        (.connectPubSub ^RedisClient        redis-cli codec))))

