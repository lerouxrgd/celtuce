(ns celtuce.cluster.async
  (:refer-clojure :exclude [get set])
  (:require 
   [celtuce.commands :refer :all])
  (:import 
   (com.lambdaworks.redis.cluster.api.async RedisAdvancedClusterAsyncCommands)))

