(ns clj-lettuce.cluster.sync
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require 
   [clj-lettuce.commands :refer :all]
   [clj-lettuce.args.zset :refer [zadd-args]]
   [clj-lettuce.args.scripting :refer [output-type]])
  (:import 
   (com.lambdaworks.redis.cluster.api.sync RedisAdvancedClusterCommands)
   (com.lambdaworks.redis 
    ScanCursor ScriptOutputType
    ScanArgs MigrateArgs SortArgs BitFieldArgs SetArgs 
    ZStoreArgs ZAddArgs ScoredValue)
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
    (.bitopAnd this d ^objects (into-array Object ks)))
  (bitop-not [this d k]
    (.bitopNot this d k))
  (bitop-or [this d ks]
    (.bitopOr this d ^objects (into-array Object ks)))
  (bitop-xor [this d ks]
    (.bitopXor this d ^objects (into-array Object ks)))
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

  ListCommands
  (blpop [this ^long sec ks]
    (let [res (.blpop this sec ^objects (into-array Object ks))]
      [(.key res) (.value res)]))
  (brpop [this ^long sec ks]
    (let [res (.brpop this sec ^objects (into-array Object ks))]
      [(.key res) (.value res)]))
  (brpoplpush [this ^long sec s d]
    (.brpoplpush this sec s d))
  (lindex [this k ^long idx]
    (.lindex this k idx))
  (linsert [this k ^Boolean b? p v]
    (.linsert this k b? p v))
  (llen [this k]
    (.llen this k))
  (lpop [this k]
    (.lpop this k))
  (lpush [this k v]
    (.lpush this k ^objects (into-array Object [v])))
  (lpushx [this k v]
    (.lpushx this k v))
  (lrange [this k ^long s ^long e]
    (into [] (.lrange this k s e)))
  (lrem [this k ^long c v]
    (.lrem this k c v))
  (lset [this k ^long idx v]
    (.lset this k idx v))
  (ltrim [this k ^long s ^long e]
    (.ltrim this k s e))
  (mrpush [this k vs]
    (.rpush this k ^objects (into-array Object vs)))
  (mlpush [this k vs]
    (.lpush this k ^objects (into-array Object vs)))
  (rpop [this k]
    (.rpop this k))
  (rpoplpush [this s d]
    (.rpoplpush this s d))
  (rpush [this k v]
    (.rpush this k ^objects (into-array Object [v])))
  (rpushx [this k v]
    (.rpushx this k v))

  SetCommands
  (msadd [this k ms]
    (.sadd this k ^objects (into-array Object ms)))
  (msrem [this k ms]
    (.srem this k ^objects (into-array Object ms)))
  (sadd [this k m]
    (.sadd this k ^objects (into-array Object [m])))
  (scard [this k]
    (.scard this k))
  (sdiff [this ks]
    (into #{} (.sdiff this ^objects (into-array Object ks))))
  (sdiffstore [this d ks]
    (.sdiffstore this d ^objects (into-array Object ks)))
  (sinter [this ks]
    (into #{} (.sinter this ^objects (into-array Object ks))))
  (sinterstore [this d ks]
    (.sinterstore this d ^objects (into-array Object ks)))
  (sismember [this k v]
    (.sismember this k v))
  (smove [this k d m]
    (.smove this k d m))
  (smembers [this k]
    (into #{} (.smembers this k)))
  (spop 
    ([this k]
     (.spop this k)) 
    ([this k ^long c]
     (into #{} (.spop this k c))))
  (srandmember 
    ([this k]
     (.srandmember this k)) 
    ([this k ^long c]
     (into #{} (.srandmember this k c))))
  (srem [this k m]
    (.srem this k ^objects (into-array Object [m])))
  (sunion [this ks]
    (into #{} (.sunion this ^objects (into-array Object ks))))
  (sunionstore [this d ks]
    (.sunionstore this d ^objects (into-array Object ks)))
  (sscan 
    ([this k]
     (.sscan this k))
    ([this k ^ScanCursor c]
     (.sscan this k c))
    ([this k ^ScanCursor c args]
     (.sscan this k c)))

  SortedSetCommands
  (zadd
    ([this k ^double s m]
     (.zadd this k s m))
    ([this k opt ^Double s m]
     (.zadd this k (zadd-args opt) s m)))
  (mzadd
    ([this k sms]
     (.zadd this k ^objects (into-array Object (mapcat identity sms))))
    ([this k opt sms]
     (.zadd this k (zadd-args opt) ^objects (into-array Object (mapcat identity sms)))))
  (zaddincr [this k ^double s m]
    (.zaddincr this k s m))
  (zcard [this k]
    (.zcard this k))
  (zcount [this k ^double min ^double max]
    (.zcount this k min max))
  (zincrby [this k ^double a m]
    (.zincrby this k a m))
  (zinterstore
    ([this d ^objects ks]
     (.zinterstore this d ks))
    ([this d ^ZStoreArgs args ^objects ks]
     (.zinterstore this d args ks)))
  (zrange [this k ^long s ^long e]
    (into [] (.zrange this k s e)))
  (zrange-withscores [this k ^long s ^long e]
    (->> (.zrangeWithScores this k s e) 
         (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
         (into [])))
  (zrangebyscore
    ([this k ^double min ^double max]
     (into [] (.zrangebyscore this k min max)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (into [] (.zrangebyscore this k min max o c))))
  (zrangebyscore-withscores
    ([this k ^double min ^double max]
     (->> (.zrangebyscoreWithScores this k min max)
          (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
          (into [])))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (->> (.zrangebyscoreWithScores this k min max o c) 
          (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
          (into []))))
  (zrank [this k m]
    (.zrank this k m))
  (zrem [this k m]
    (.zrem this k ^objects (into-array Object [m])))
  (mzrem [this k ms]
    (.zrem this k ^objects (into-array Object ms)))
  (zremrangebyrank [this k ^long s ^long e]
    (.zremrangebyrank this k s e))
  (zremrangebyscore [this k ^Double min ^Double max]
    (.zremrangebyscore this k min max))
  (zrevrange [this k ^long s ^long e]
    (into [] (.zrevrange this k s e)))
  (zrevrange-withscores [this k ^long s ^long e]
    (->> (.zrevrangeWithScores this k s e)
         (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
         (into [])))
  (zrevrangebyscore
    ([this k ^double min ^double max]
     (into [] (.zrevrangebyscore this k min max)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (into [] (.zrevrangebyscore this k min max o c))))
  (zrevrangebyscore-withscores
    ([this k ^double min ^double max]
     (->> (.zrevrangebyscoreWithScores this k min max)
          (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
          (into [])))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (->> (.zrevrangebyscoreWithScores this k min max o c)
          (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
          (into []))))
  (zrevrank [this k m]
    (.zrevrank this k m))
  (zscore [this k m]
    (.zscore this k m))
  (zunionstore
    ([this d ks]
     (.zunionstore this d ^objects (into-array Object ks)))
    ([this d ^ZStoreArgs args ks]
     (.zunionstore this d args ^objects (into-array Object ks))))
  (zscan
    ([this k]
     (.zscan this k))
    ([this k ^ScanCursor c]
     (.zscan this k c))
    ([this k ^ScanCursor c ^ScanArgs args]
     (.zscan this c args)))
  (zlexcount [this k ^String min ^String max]
    (.zlexcount this k min max))
  (zremrangebylex [this k ^String min ^String max]
    (.zremrangebylex this k min max))
  (zrangebylex 
    ([this k ^String min ^String max]
     (into [] (.zrangebylex this k min max)))
    ([this k ^String min ^String max ^Long o ^Long c]
     (into [] (.zrangebylex this k min max o c))))

  ScriptingCommands
  (eval 
    ([this ^String script t ks]
     (.eval this script (output-type t) ^objects (into-array Object ks)))
    ([this ^String script t ks vs]
     (.eval this script (output-type t) 
            ^objects (into-array Object ks)
            ^objects (into-array Object vs))))
  (evalsha 
    ([this ^String digest t ks]
     (.evalsha this digest (output-type t) ^objects (into-array Object ks)))
    ([this ^String digest t ks vs]
     (.evalsha this digest (output-type t) 
               ^objects (into-array Object ks)
               ^objects (into-array Object vs))))
  (script-exists? [this digests]
    (.scriptExists this ^"[Ljava.lang.String;" (into-array String digests)))
  (script-flush [this]
    (.scriptFlush this))
  (script-kill [this]
    (.scriptKill this))
  (scirpt-load [this script]
    (.scriptLoad this script))
  (digest [this script]
    (.digest this script))

  ServerCommands
  (bgrewriteaof [this]
    )
  (bgsave [this]
    )
  (client-getname [this]
    )
  (client-setname [this name]
    )
  (client-kill [this addr-or-args]
    )
  (client-pause [this timeout-ms]
    )
  (client-list [this]
    )
  (command [this]
    )
  (command-info [this commands]
    )
  (command-count [this]
    )
  (config-get [this param]
    )
  (config-resetstat [this]
    )
  (config-rewrite [this]
    )
  (config-set [this param val]
    )
  (dbsize [this]
    )
  (debug-crash-and-recover [this delay-ms]
    )
  (debug-htstats [this db]
    )
  (debug-object [this key]
    )
  (debug-oom [this]
    )
  (debug-segfault [this]
    )
  (debug-reload [this]
    )
  (debug-restart [this delay-ms]
    )
  (debug-sds-len [this key]
    )
  (flushall [this]
    (.flushall this))
  (flushall-async [this]
    )
  (flushdb [this]
    )
  (flushdb-async [this]
    )
  (info 
    ([this]) 
    ([this section]))
  (lastsave [this]
    )
  (save [this]
    )
  (shutdown [this save?]
    )
  (slave-of [this host port]
    )
  (slave-no-one [this]
    )
  (slowlog-get 
    ([this]) 
    ([this count]))
  (slowlog-len [this]
    )
  (slowlog-reset [this]
    )
  (time [this]
    )

  )
