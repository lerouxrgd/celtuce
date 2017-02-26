(ns celtuce.connector
  (:require 
   [celtuce.codec :refer [nippy-codec]])
  (:import 
   (java.util.concurrent TimeUnit)
   (com.lambdaworks.redis.codec RedisCodec)
   (com.lambdaworks.redis RedisClient)
   (com.lambdaworks.redis.api StatefulRedisConnection)
   (com.lambdaworks.redis.cluster RedisClusterClient)
   (com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection)))

(defprotocol RedisConnector
  "Manipulate Redis stateful connection"
  (commands-sync [this])
  (commands-async [this])
  (flush-commands [this])
  (reset [this])
  (shutdown [this]))

;;
;; Redis Server
;;

(defrecord RedisServer 
    [^RedisClient redis-cli ^StatefulRedisConnection stateful-conn]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.impl.server.sync])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.impl.server.async])
    (.async stateful-conn))
  (flush-commands [this] 
    (.flushCommands stateful-conn ))
  (reset [this] 
    (.reset stateful-conn))
  (shutdown [this]
    (.close stateful-conn)
    (.shutdown redis-cli)))

(defn redis-server
  [^String redis-uri & 
   {timeout :timeout unit :unit codec :codec auto-flush :auto-flush
    :or {unit       TimeUnit/MILLISECONDS 
         codec      (nippy-codec) 
         auto-flush true}}]
  (let [^RedisClient redis-cli
        (RedisClient/create redis-uri)
        ^StatefulRedisConnection stateful-conn 
        (.connect redis-cli ^RedisCodec codec)]
    (when (and timeout unit)
      (.setTimeout stateful-conn timeout unit))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (->RedisServer redis-cli stateful-conn)))

;;
;; Redis Cluster
;;

(defrecord RedisCluster 
    [^RedisClusterClient redis-cli ^StatefulRedisClusterConnection stateful-conn]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.impl.cluster.sync])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.impl.cluster.async])
    (.async stateful-conn))
  (flush-commands [this] 
    (.flushCommands stateful-conn ))
  (reset [this] 
    (.reset stateful-conn))
  (shutdown [this]
    (.close stateful-conn)
    (.shutdown redis-cli)))

(defn redis-cluster
  [^String redis-uri & 
   {timeout :timeout unit :unit codec :codec auto-flush :auto-flush
    :or {unit       TimeUnit/MILLISECONDS 
         codec      (nippy-codec) 
         auto-flush true}}]
  (let [redis-cli (RedisClusterClient/create redis-uri)
        stateful-conn (.connect redis-cli codec)]
    (when (and timeout unit)
      (.setTimeout stateful-conn timeout unit))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (->RedisCluster redis-cli stateful-conn)))

