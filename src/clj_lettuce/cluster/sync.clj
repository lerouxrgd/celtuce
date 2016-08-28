(ns clj-lettuce.cluster.sync
  (:refer-clojure :exclude [get set keys sort type])
  (:require 
   [clj-lettuce.commands :refer :all])
  (:import 
   (com.lambdaworks.redis.cluster.api.sync RedisAdvancedClusterCommands)
   (com.lambdaworks.redis ScanArgs ScanCursor MigrateArgs SortArgs)
   (java.util Map)))

(extend-type RedisAdvancedClusterCommands
  
  HashCommands
  (hdel [this k f]
    (.hdel this k ^"[Ljava.lang.Object;" (into-array Object [f])))
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
  (hscan 
    ([this k]
     (.hscan this k))
    ([this k ^ScanCursor c]
     (.hscan this k c))
    ([this k ^ScanCursor c ^ScanArgs args]
     (.hscan this k c args)))
  (hset [this k f v]
    (.hset this k f v))
  (hsetnx [this k f v]
    (.hsetnx this k f v))
  (hstrlen [this k f]
    (.hstrlen this k f))
  (hvals [this k]
    (into [] (.hvals this k)))

  KeysCommands
  (del [this k]
    (.del this (into-array Object [k])))
  (dump [this k]
    (.dump this k))
  (exists [this k]
    (.exists this (into-array Object [k])))
  (expire [this k ^long sec]
    (.expire this k sec))
  (expireat [this k ts-sec]
    (.expire this k ^long ts-sec))
  (keys [this pattern]
    (.keys this pattern))
  (mdel [this ks]
    (.del this (into-array Object ks)))
  (mexists [this ks]
    (.exists this (into-array Object ks)))
  (migrate [this ^String h ^Integer p ^Integer db ^Long ms ^MigrateArgs args]
    (.migrate this h p db ms args))
  (move [this k ^Integer db]
    (.move this k db))
  (mtouch [this ks]
    (.touch this (into-array Object ks)))
  (munlink [this ks]
    (.unlink this (into-array Object ks)))
  (object [this]
    )
  (persist [this k]
    (.persist this k))
  (pexpire [this k ^long ms]
    (.pexpire this k ms))
  (pexpireat [this k ^long ts-ms]
    (.pexpireat this k ts-ms))
  (pttl [this k]
    (.pttl this k))
  (randomkey [this]
    (.randomkey this))
  (rename [this k1 k2]
    (.rename this k1 k2))
  (renamenx [this k1 k2]
    (.renamenx this k1 k2))
  (restore [this k ^long ttl ^bytes v]
    (.restore this k ttl v))
  (scan 
    ([this]
     (.scan this))
    ([this ^ScanCursor c]
     (.scan this c))
    ([this ^ScanCursor c ^ScanArgs args]
     (.scan this c args)))
  (sort
    ([this k]
     (.sort this k)) 
    ([this k ^SortArgs args]
     (.sort this k args)))
  (sortstore [this k1 ^SortArgs args k2]
    (.sortStore this k1 args k2))
  (touch [this k]
    (.touch this (into-array Object [k])))
  (ttl [this k]
    (.ttl this k))
  (type [this k]
    (.type this k))
  (unlink [this k]
    (.unlink this (into-array Object [k])))

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
