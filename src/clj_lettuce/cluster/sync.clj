(ns clj-lettuce.cluster.sync
  (:refer-clojure :exclude [get set])
  (:require [clj-lettuce.commands :refer :all])
  (:import [com.lambdaworks.redis.cluster.api.sync RedisAdvancedClusterCommands]))

(extend-type RedisAdvancedClusterCommands
  StringsCommands
  (get [this k] 
    (.get this k))
  (set [this k v] 
    (.set this k v))
  (mget [this ks] 
    (into (empty ks) (.mget this (into-array ks))))
  )
