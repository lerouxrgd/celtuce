(ns clj-lettuce.cluster.sync
  (:refer-clojure :exclude [get set])
  (:require [clj-lettuce.commands :refer :all])
  (:import [com.lambdaworks.redis.cluster.api.sync RedisAdvancedClusterCommands]
           [java.util Map]))

(extend-type RedisAdvancedClusterCommands
  
  HashCommands
  (hdel [this k f]
    (.hdel this k ^"[Ljava.lang.Object;" (into-array [f])))
  (hmdel [this k fs]
    (.hdel this k ^"[Ljava.lang.Object;" (into-array Object fs)))
  (hexists [this k f]
    (.hexists this k f))
  (hget [this k f]
    (.hget this k f))
  (hincrby [this k f a]
    (.hincrby this k f (long a)))
  (hincrbyfloat [this k f a]
    (.hincrbyfloat this k f (double a)))
  (hgetall [this k]
    (into {} (.hgetall this k)))
  (hkeys [this k]
    (into [] (.hkeys this k)))
  (hlen [this k]
    (.hlen this k))
  (hmget [this k fs]
    (into (empty fs) (.hmget this k ^"[Ljava.lang.Object;" (into-array Object fs))))
  (hmset [this k ^Map m]
    (.hmset this k m))
  (hscan [this k]
    (.hscan this k))
  (hset [this k f v]
    (.hset this k f v))
  (hsetnx [this k f v]
    (.hsetnx this k f v))
  (hstrlen [this k f]
    (.hstrlen this k f))
  (hvals [this k]
    (into [] (.hvals this k)))
  
  StringsCommands
  (get [this k] 
    (.get this k))
  (set [this k v] 
    (.set this k v))
  (mget [this ks] 
    (into (empty ks) (.mget this (into-array Object ks))))
  
  ServerCommands
  (flushall [this]
    (.flushall this))
  
  )
