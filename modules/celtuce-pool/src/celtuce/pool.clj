(ns celtuce.pool
  (:import
   (java.util.function Supplier)
   (io.lettuce.core RedisClient)
   (io.lettuce.core.cluster RedisClusterClient)
   (io.lettuce.core.codec RedisCodec)
   (io.lettuce.core.api StatefulRedisConnection)
   (io.lettuce.core.cluster.api StatefulRedisClusterConnection)
   (io.lettuce.core.pubsub StatefulRedisPubSubConnection)   
   (io.lettuce.core.support ConnectionPoolSupport)
   (org.apache.commons.pool2.impl GenericObjectPool GenericObjectPoolConfig)))

(defprotocol ConnectionPool
  "Functions for using the connection pool"
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
  "Internal helper to build GenericObjectPoolConfig from a map"
  [{:keys [max-total max-idle min-idle]
    :or {max-total GenericObjectPoolConfig/DEFAULT_MAX_TOTAL
         max-idle  GenericObjectPoolConfig/DEFAULT_MAX_IDLE
         min-idle  GenericObjectPoolConfig/DEFAULT_MIN_IDLE}}]
  (doto (GenericObjectPoolConfig.)
    (.setMaxTotal max-total)
    (.setMaxIdle  max-idle)
    (.setMinIdle  min-idle)))

(defn conn-pool
  "Create a ConnectionPoolImpl that wraps a ConnectionPoolSupport.
  Takes a connector and a command function that will be called on pooled connections.
  Last parameter (options) allows to fully customize your pool based on Apache2 GenericObjectPoolConfig;
  you have two options: pass a map with keys max-idle, min-idle and max-total or a org.apache.commons.pool2.impl.GenericObjectPoolConfig"
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
     (if (map? options)
       (pool-config options)
       options))
    connector
    cmds-fn)))

(defmacro with-conn-pool
  "Takes a ConnectionPool `coon-pool` and a `cmds-name` symbol that will be bound to
  the command function of the pool called on a borrowed connection"
  [conn-pool cmds-name & body]
  `(let [conn# (borrow-conn ~conn-pool)
         ~cmds-name ((:cmds-fn ~conn-pool)
                     (assoc (:connector ~conn-pool) :stateful-conn conn#))]
     (try
       ~@body
       (finally (return-conn ~conn-pool conn#)))))

(defmacro with-conn-pool*
  "Like with-conn-pool but also binds the pooled connection to `conn-name`.
  User is responsible for returning it to the pool within `body`"
  [conn-pool cmds-name conn-name & body]
  `(let [~conn-name (borrow-conn ~conn-pool)
         ~cmds-name ((:cmds-fn ~conn-pool)
                     (assoc (:connector ~conn-pool) :stateful-conn ~conn-name))]
     ~@body))
