(ns celtuce.connector
  (:require 
   [celtuce.codec :refer [nippy-codec]])
  (:import 
   (java.util.concurrent TimeUnit)
   (com.lambdaworks.redis.codec RedisCodec)
   (com.lambdaworks.redis.cluster RedisClusterClient)
   (com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection)))

(defprotocol RedisConnector
  ""  
  (commands-sync [this])
  (commands-async [this])
  (flush-commands [this])
  (reset [this])
  (shutdown [this]))

;;
;; Redis Cluster
;;

(defrecord RedisCluster 
    [^RedisClusterClient redis-cli ^StatefulRedisClusterConnection stateful-conn]
  RedisConnector
  (commands-sync [this]
    (require '[celtuce.cluster.sync])
    (.sync stateful-conn))
  (commands-async [this]
    (require '[celtuce.cluster.async])
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

