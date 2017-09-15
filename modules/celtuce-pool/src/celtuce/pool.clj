(ns celtuce.pool
  (:import
   (java.util.function Supplier)
   (com.lambdaworks.redis RedisClient)
   (com.lambdaworks.redis.cluster RedisClusterClient)
   (com.lambdaworks.redis.codec RedisCodec)
   (com.lambdaworks.redis.api StatefulRedisConnection)
   (com.lambdaworks.redis.cluster.api StatefulRedisClusterConnection)
   (com.lambdaworks.redis.pubsub StatefulRedisPubSubConnection)   
   (com.lambdaworks.redis.support ConnectionPoolSupport)
   (org.apache.commons.pool2.impl GenericObjectPool GenericObjectPoolConfig)))

(defprotocol ConnectionPool
  ""
  (borrow-conn [this])
  (return-conn [this conn])
  (close       [this]))

(defrecord ConnectionPoolImpl [^GenericObjectPool conn-pool connector cmds-fn]
  ConnectionPool
  (borrow-conn [_]
    (.borrowObject conn-pool))
  (return-conn [_ conn]
    (.returnObject conn-pool conn))
  (close [_]
    (.close conn-pool)))

(defn ^GenericObjectPoolConfig pool-config
  ""
  [{:keys [max-total max-idle min-idle]
    :or {max-total GenericObjectPoolConfig/DEFAULT_MAX_TOTAL
         max-idle  GenericObjectPoolConfig/DEFAULT_MAX_IDLE
         min-idle  GenericObjectPoolConfig/DEFAULT_MIN_IDLE}}]
  (doto (GenericObjectPoolConfig.)
    (.setMaxTotal max-total)
    (.setMaxIdle  max-total)
    (.setMinIdle  min-idle)))

(defn conn-pool
  ""
  ([connector cmds-fn]
   (conn-pool connector cmds-fn {}))
  ([{:keys [redis-client stateful-conn codec] :as connector} cmds-fn options]
   (->ConnectionPoolImpl
    (ConnectionPoolSupport/createGenericObjectPool
     (reify Supplier
       (get [this]
         (condp instance? stateful-conn
           StatefulRedisConnection
           (.connect ^RedisClient redis-client ^RedisCodec codec)
           StatefulRedisClusterConnection
           (.connect ^RedisClusterClient redis-client codec)
           StatefulRedisPubSubConnection
           (condp instance? redis-client
             RedisClusterClient
             (.connectPubSub ^RedisClusterClient redis-client codec)
             RedisClient
             (.connectPubSub ^RedisClient redis-client ^RedisCodec codec)))))
     (pool-config options))
    connector
    cmds-fn)))

(defmacro with-conn-pool
  ""
  [conn-pool cmds-name & body]
  `(let [conn# (borrow-conn ~conn-pool)
         ~cmds-name ((:cmds-fn ~conn-pool)
                     (assoc (:connector ~conn-pool) :stateful-conn conn#))]
     (try
       ~@body
       (finally (return-conn ~conn-pool conn#)))))
