(ns celtuce.manifold
  (:require
   [celtuce.connector])
  (:import
   (celtuce.connector
    RedisServer RedisCluster RedisPubSub)
   (io.lettuce.core.api StatefulRedisConnection)
   (io.lettuce.core.cluster.api StatefulRedisClusterConnection)
   (io.lettuce.core.pubsub StatefulRedisPubSubConnection)))

(defprotocol CommandsManifold
  "Adds support for manifold based asynchronous commands"
  (commands-manifold [this]))

(extend-protocol CommandsManifold
  RedisServer
  (commands-manifold [this]
    (require '[celtuce.manifold.scan])
    (require '[celtuce.manifold.server])
    (.async ^StatefulRedisConnection (:stateful-conn this)))
  RedisCluster
  (commands-manifold [this]
    (require '[celtuce.manifold.scan])
    (require '[celtuce.manifold.cluster])
    (.async ^StatefulRedisClusterConnection (:stateful-conn this)))
  RedisPubSub
  (commands-manifold [this]
    (require '[celtuce.manifold.scan])
    (require '[celtuce.manifold.pubsub])
    (.async ^StatefulRedisPubSubConnection (:stateful-conn this))))
