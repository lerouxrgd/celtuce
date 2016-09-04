(ns clj-lettuce.cluster.sync
  (:refer-clojure :exclude [get set keys sort type])
  (:require 
   [clj-lettuce.commands :refer :all])
  (:import 
   (com.lambdaworks.redis.cluster.api.sync RedisAdvancedClusterCommands)
   (com.lambdaworks.redis 
    ScanArgs ScanCursor MigrateArgs SortArgs BitFieldArgs SetArgs)
   (java.util Map)))

(extend-type RedisAdvancedClusterCommands
  
  HashCommands
  (hdel [this k f]
    (.hdel this k ^objects (into-array Object [f])))
  (hmdel [this k fs]
    (.hdel this k ^objects (into-array Object fs)))
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
    (into (empty fs) (.hmget this k ^objects (into-array Object fs))))
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

  KeyCommands
  (del [this k]
    (.del this (into-array Object [k])))
  (dump [this k]
    (.dump this k))
  (exists [this k]
    (.exists this ^objects (into-array Object [k])))
  (expire [this k ^long sec]
    (.expire this k sec))
  (expireat [this k ts-sec]
    (.expire this k ^long ts-sec))
  (keys [this pattern]
    (into [] (.keys this pattern)))
  (mdel [this ks]
    (.del this (into-array Object ks)))
  (mexists [this ks]
    (.exists this ^objects (into-array Object ks)))
  (migrate [this ^String h ^Integer p ^Integer db ^Long ms ^MigrateArgs args]
    (.migrate this h p db ms args))
  (move [this k ^Integer db]
    (.move this k db))
  (mtouch [this ks]
    (.touch this (into-array Object ks)))
  (munlink [this ks]
    (.unlink this (into-array Object ks)))
  (obj-encoding [this k]
    (.objectEncoding this k))
  (obj-idletime [this k]
    (.objectIdletime this k))
  (obj-refcount [this k]
    (.objectRefcount this k))
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
  (sort-store [this k ^SortArgs args d]
    (.sortStore this k args d))
  (touch [this k]
    (.touch this (into-array Object [k])))
  (ttl [this k]
    (.ttl this k))
  (type [this k]
    (.type this k))
  (unlink [this k]
    (.unlink this (into-array Object [k])))

  StringsCommands
  (append [this k v]
    (.append this k v))
  (bitcount 
    ([this k]
     (.bitcount this k))
    ([this k ^long s ^long e]
     (.bitcount this k s e)))
  (bitfield [this k ^BitFieldArgs args]
    (into [] (.bitfield this k args)))
  (bitop-and [this d ks]
    (.bitopAnd this d ^objects (into-array Object [ks])))
  (bitop-not [this d k]
    (.bitopNot this d k))
  (bitop-or [this d ks]
    (.bitopOr this d ^objects (into-array Object [ks])))
  (bitop-xor [this d ks]
    (.bitopXor this d ^objects (into-array Object [ks])))
  (bitpos 
    ([this k ^Boolean state]
     (.bitpos this k state)) 
    ([this k ^Boolean state ^Long s ^Long e]
     (.bitpos this k state s e)))
  (decr [this k]
    (.decr this k))
  (decrby [this k ^long a]
    (.decrby this k a))
  (get [this k]
    (.get this k))
  (getbit [this k ^long o]
    (.getbit this k o))
  (getrange [this k ^long s ^long e]
    (.getrange this k s e))
  (getset [this k v]
    (.getset this k v))
  (incr [this k]
    (.incr this k))
  (incrby [this k ^long a]
    (.incrby this k a))
  (incrbyfloat [this k ^double a]
    (.incrbyfloat this k a))
  (mget [this ks]
    (into (empty ks) (.mget this (into-array Object ks))))
  (mset [this m]
    (.mset this m))
  (msetnx [this m]
    (.msetnx this m))
  (set 
    ([this k v]
     (.set this k v))
    ([this k v ^SetArgs args]
     (.set this k v args)))
  (setbit [this k ^Long o ^Integer v]
    (.setbit this k o v))
  (setex [this k ^long sec v]
    (.setex this k sec v))
  (psetex [this k ^long ms v]
    (.psetex this k ms v))
  (setnx [this k v]
    (.setnx this k v))
  (setrange [this k ^long o v]
    (.setrange this k o v))
  (strlen [this k]
    (.strlen this k))
  
  ServerCommands
  (flushall [this]
    (.flushall this))

  )
