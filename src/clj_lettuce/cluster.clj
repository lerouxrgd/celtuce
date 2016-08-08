(ns clj-lettuce.cluster
  (:require [clj-lettuce.commands :refer [mk-commands]]
            clj-lettuce.cluster.sync
            clj-lettuce.cluster.async)
  (:import [com.lambdaworks.redis.cluster RedisClusterClient]
           [com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection]))

(defmethod mk-commands :cluster-sync 
  [type ^StatefulRedisClusterConnection cluster-conn]
  "Returns RedisAdvancedClusterCommands"
  (.sync cluster-conn))

(defmethod mk-commands :cluster-async
  [type ^StatefulRedisClusterConnection cluster-conn]
  "Returns RedisAdvancedClusterAsyncCommands"
  (.async cluster-conn))

(defn redis-cli [^String redis-uri]
  (RedisClusterClient/create redis-uri))

(defn stateful-conn [^RedisClusterClient cluster-cli]
  (.connect cluster-cli))

(defn close-conn [^StatefulRedisClusterConnection cluster-conn]
  (.close cluster-conn))

(defn shutdown [^RedisClusterClient cluster-cli]
  (.shutdown cluster-cli))
