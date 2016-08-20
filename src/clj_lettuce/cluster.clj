(ns clj-lettuce.cluster
  (:require [clj-lettuce.commands :refer [RedisConnector commands stateful-conn]]
            [clj-lettuce.codec :refer [nippy-codec]]
            clj-lettuce.cluster.sync
            clj-lettuce.cluster.async)
  (:import [com.lambdaworks.redis.cluster RedisClusterClient]
           [com.lambdaworks.redis.codec RedisCodec]
           [com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection]))

(defrecord RedisCluster [redis-cli stateful-conn]
  RedisConnector
  (redis-cli [this] 
    redis-cli)
  (stateful-conn [this] 
    stateful-conn)
  (conn-open? [this]
    (.isOpen ^StatefulRedisClusterConnection stateful-conn))
  (close-conn [this]
    (.close ^StatefulRedisClusterConnection stateful-conn))
  (shutdown [this]
    (.shutdown ^RedisClusterClient redis-cli)))

(defn redis-cluster 
  ([redis-uri]
   (redis-cluster redis-uri (nippy-codec)))
  ([^String redis-uri ^RedisCodec codec]
   (let [redis-cli (RedisClusterClient/create redis-uri)
         stateful-conn (.connect redis-cli codec)]
     (->RedisCluster redis-cli stateful-conn))))

(defmethod commands :cluster-sync 
  [type redis-connector]
  "Returns RedisAdvancedClusterCommands"
  (.sync ^StatefulRedisClusterConnection (stateful-conn redis-connector)))

(defmethod commands :cluster-async
  [type redis-connector]
  "Returns RedisAdvancedClusterAsyncCommands"
  (.async ^StatefulRedisClusterConnection (stateful-conn redis-connector)))
