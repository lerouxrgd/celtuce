(ns celtuce.server.async
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require 
   [celtuce.commands :refer :all]
   [manifold.deferred :as d])
  (:import 
   (com.lambdaworks.redis.api.async RedisAsyncCommands)))

(extend-type RedisAsyncCommands
 
  HashCommands
  (hget [this k f]
    (d/->deferred (.hget this k f)))
  (hset [this k f v]
    (d/->deferred (.hset this k f v)))
  
  ServerCommands
  (flushall [this]
    (.flushall this))
  
  )
