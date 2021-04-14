(ns celtuce.manifold.cluster
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require
   [celtuce.commands :refer :all]
   [celtuce.args.zset :refer [zadd-args]]
   [celtuce.args.scripting :refer [output-type]]
   [celtuce.args.geo :refer [->unit]]
   [manifold.deferred :as d])
  (:import
   (io.lettuce.core.cluster.api.async RedisAdvancedClusterAsyncCommands)
   (io.lettuce.core
    Value KeyValue ScanCursor KeyValue
    ScanArgs MigrateArgs SortArgs BitFieldArgs SetArgs KillArgs
    ZStoreArgs ScoredValue
    GeoArgs GeoRadiusStoreArgs GeoWithin GeoCoordinates)
   (java.util Map)))

(extend-type RedisAdvancedClusterAsyncCommands

  ConnectionCommands
  (ping [this]
    (d/->deferred (.ping this)))
  (echo [this val]
    (d/->deferred (.echo this val)))

  HashCommands
  (hdel [this k f]
    (d/->deferred (.hdel this k ^objects (into-array Object [f]))))
  (hmdel [this k fs]
    (d/->deferred (.hdel this k ^objects (into-array Object fs))))
  (hexists [this k f]
    (d/->deferred (.hexists this k f)))
  (hget [this k f]
    (d/->deferred (.hget this k f)))
  (hincrby [this k f a]
    (d/->deferred (.hincrby this k f (long a))))
  (hincrbyfloat [this k f a]
    (d/->deferred (.hincrbyfloat this k f (double a))))
  (hgetall [this k]
    (d/chain (d/->deferred (.hgetall this k))
             #(into {} %)))
  (hkeys [this k]
    (d/chain (d/->deferred (.hkeys this k))
             #(into [] %)))
  (hlen [this k]
    (d/->deferred (.hlen this k)))
  (hmget [this k fs]
    (d/chain (d/->deferred (.hmget this k ^objects (into-array Object fs)))
             #(map (fn [^KeyValue kv] (.getValueOrElse kv nil)) %)
             #(into (empty fs) %)))
  (hmset [this k ^Map m]
    (d/->deferred (.hmset this k m)))
  (hscan
    ([this k]
     (d/->deferred (.hscan this k)))
    ([this k ^ScanCursor c]
     (d/->deferred (.hscan this k c)))
    ([this k ^ScanCursor c ^ScanArgs args]
     (d/->deferred (.hscan this k c args))))
  (hset [this k f v]
    (d/->deferred (.hset this k f v)))
  (hsetnx [this k f v]
    (d/->deferred (.hsetnx this k f v)))
  (hstrlen [this k f]
    (d/->deferred (.hstrlen this k f)))
  (hvals [this k]
    (d/chain (d/->deferred (.hvals this k))
             #(into [] %)))

  KeyCommands
  (del [this k]
    (d/->deferred (.del this (into-array Object [k]))))
  (dump [this k]
    (d/->deferred (.dump this k)))
  (exists [this k]
    (d/->deferred (.exists this ^objects (into-array Object [k]))))
  (expire [this k ^long sec]
    (d/->deferred (.expire this k sec)))
  (expireat [this k ts-sec]
    (d/->deferred (.expire this k ^long ts-sec)))
  (keys [this pattern]
    (d/chain (d/->deferred (.keys this pattern))
             #(into [] %)))
  (mdel [this ks]
    (d/->deferred (.del this (into-array Object ks))))
  (mexists [this ks]
    (d/->deferred (.exists this ^objects (into-array Object ks))))
  (migrate [this ^String h ^Integer p ^Integer db ^Long ms ^MigrateArgs args]
    (d/->deferred (.migrate this h p db ms args)))
  (move [this k ^Integer db]
    (d/->deferred (.move this k db)))
  (mtouch [this ks]
    (d/->deferred (.touch this (into-array Object ks))))
  (munlink [this ks]
    (d/->deferred (.unlink this (into-array Object ks))))
  (obj-encoding [this k]
    (d/->deferred (.objectEncoding this k)))
  (obj-idletime [this k]
    (d/->deferred (.objectIdletime this k)))
  (obj-refcount [this k]
    (d/->deferred (.objectRefcount this k)))
  (persist [this k]
    (d/->deferred (.persist this k)))
  (pexpire [this k ^long ms]
    (d/->deferred (.pexpire this k ms)))
  (pexpireat [this k ^long ts-ms]
    (d/->deferred (.pexpireat this k ts-ms)))
  (pttl [this k]
    (d/->deferred (.pttl this k)))
  (randomkey [this]
    (d/->deferred (.randomkey this)))
  (rename [this k1 k2]
    (d/->deferred (.rename this k1 k2)))
  (renamenx [this k1 k2]
    (d/->deferred (.renamenx this k1 k2)))
  (restore [this k ^long ttl ^bytes v]
    (d/->deferred (.restore this k ttl v)))
  (scan
    ([this]
     (d/->deferred (.scan this)))
    ([this ^ScanCursor c]
     (d/->deferred (.scan this c)))
    ([this ^ScanCursor c ^ScanArgs args]
     (d/->deferred (.scan this c args))))
  (sort
    ([this k]
     (d/->deferred (.sort this k)))
    ([this k ^SortArgs args]
     (d/->deferred (.sort this k args))))
  (sort-store [this k ^SortArgs args d]
    (d/->deferred (.sortStore this k args d)))
  (touch [this k]
    (d/->deferred (.touch this (into-array Object [k]))))
  (ttl [this k]
    (d/->deferred (.ttl this k)))
  (type [this k]
    (d/->deferred (.type this k)))
  (unlink [this k]
    (d/->deferred (.unlink this (into-array Object [k]))))

  StringsCommands
  (append [this k v]
    (d/->deferred (.append this k v)))
  (bitcount
    ([this k]
     (d/->deferred (.bitcount this k)))
    ([this k ^long s ^long e]
     (d/->deferred (.bitcount this k s e))))
  (bitfield [this k ^BitFieldArgs args]
    (d/chain (d/->deferred (.bitfield this k args))
             #(into [] %)))
  (bitop-and [this d ks]
    (d/->deferred (.bitopAnd this d ^objects (into-array Object ks))))
  (bitop-not [this d k]
    (d/->deferred (.bitopNot this d k)))
  (bitop-or [this d ks]
    (d/->deferred (.bitopOr this d ^objects (into-array Object ks))))
  (bitop-xor [this d ks]
    (d/->deferred (.bitopXor this d ^objects (into-array Object ks))))
  (bitpos
    ([this k ^Boolean state]
     (d/->deferred (.bitpos this k state)))
    ([this k ^Boolean state ^Long s ^Long e]
     (d/->deferred (.bitpos this k state s e))))
  (decr [this k]
    (d/->deferred (.decr this k)))
  (decrby [this k ^long a]
    (d/->deferred (.decrby this k a)))
  (get [this k]
    (d/->deferred (.get this k)))
  (getbit [this k ^long o]
    (d/->deferred (.getbit this k o)))
  (getrange [this k ^long s ^long e]
    (d/->deferred (.getrange this k s e)))
  (getset [this k v]
    (d/->deferred (.getset this k v)))
  (incr [this k]
    (d/->deferred (.incr this k)))
  (incrby [this k ^long a]
    (d/->deferred (.incrby this k a)))
  (incrbyfloat [this k ^double a]
    (d/->deferred (.incrbyfloat this k a)))
  (mget [this ks]
    (d/chain (d/->deferred (.mget this (into-array Object ks)))
             #(map (fn [^KeyValue kv] (.getValueOrElse kv nil)) %)
             #(into (empty ks) %)))
  (mset [this m]
    (d/->deferred (.mset this m)))
  (msetnx [this m]
    (d/->deferred (.msetnx this m)))
  (set
    ([this k v]
     (d/->deferred (.set this k v)))
    ([this k v ^SetArgs args]
     (d/->deferred (.set this k v args))))
  (setbit [this k ^Long o ^Integer v]
    (d/->deferred (.setbit this k o v)))
  (setex [this k ^long sec v]
    (d/->deferred (.setex this k sec v)))
  (psetex [this k ^long ms v]
    (d/->deferred (.psetex this k ms v)))
  (setnx [this k v]
    (d/->deferred (.setnx this k v)))
  (setrange [this k ^long o v]
    (d/->deferred (.setrange this k o v)))
  (strlen [this k]
    (d/->deferred (.strlen this k)))

  ListCommands
  (blpop [this ^long sec ks]
    (d/chain (d/->deferred (.blpop this sec ^objects (into-array Object ks)))
             (fn [^KeyValue res]
               (when res [(.getKey res) (.getValue res)]))))
  (brpop [this ^long sec ks]
    (d/chain (d/->deferred (.brpop this sec ^objects (into-array Object ks)))
             (fn [^KeyValue res]
               (when res [(.getKey res) (.getValue res)]))))
  (brpoplpush [this ^long sec s d]
    (d/->deferred (.brpoplpush this sec s d)))
  (lindex [this k ^long idx]
    (d/->deferred (.lindex this k idx)))
  (linsert [this k ^Boolean b? p v]
    (d/->deferred (.linsert this k b? p v)))
  (llen [this k]
    (d/->deferred (.llen this k)))
  (lpop [this k]
    (d/->deferred (.lpop this k)))
  (lpush [this k v]
    (d/->deferred (.lpush this k ^objects (into-array Object [v]))))
  (lpushx [this k v]
    (d/->deferred (.lpushx this k ^objects (into-array Object [v]))))
  (lrange [this k ^long s ^long e]
    (d/chain (d/->deferred (.lrange this k s e))
             #(into [] %)))
  (lrem [this k ^long c v]
    (d/->deferred (.lrem this k c v)))
  (lset [this k ^long idx v]
    (d/->deferred (.lset this k idx v)))
  (ltrim [this k ^long s ^long e]
    (d/->deferred (.ltrim this k s e)))
  (mrpush [this k vs]
    (d/->deferred (.rpush this k ^objects (into-array Object vs))))
  (mrpushx [this k vs]
    (d/->deferred (.rpushx this k ^objects (into-array Object vs))))
  (mlpush [this k vs]
    (d/->deferred (.lpush this k ^objects (into-array Object vs))))
  (mlpushx [this k vs]
    (d/->deferred (.lpushx this k ^objects (into-array Object vs))))
  (rpop [this k]
    (d/->deferred (.rpop this k)))
  (rpoplpush [this s d]
    (d/->deferred (.rpoplpush this s d)))
  (rpush [this k v]
    (d/->deferred (.rpush this k ^objects (into-array Object [v]))))
  (rpushx [this k v]
    (d/->deferred (.rpushx this k ^objects (into-array Object [v]))))

  SetCommands
  (msadd [this k ms]
    (d/->deferred (.sadd this k ^objects (into-array Object ms))))
  (msrem [this k ms]
    (d/->deferred (.srem this k ^objects (into-array Object ms))))
  (sadd [this k m]
    (d/->deferred (.sadd this k ^objects (into-array Object [m]))))
  (scard [this k]
    (d/->deferred (.scard this k)))
  (sdiff [this ks]
    (d/chain (d/->deferred (.sdiff this ^objects (into-array Object ks)))
             #(into #{} %)))
  (sdiffstore [this d ks]
    (d/->deferred (.sdiffstore this d ^objects (into-array Object ks))))
  (sinter [this ks]
    (d/chain (d/->deferred (.sinter this ^objects (into-array Object ks)))
             #(into #{} %)))
  (sinterstore [this d ks]
    (d/->deferred (.sinterstore this d ^objects (into-array Object ks))))
  (sismember [this k v]
    (d/->deferred (.sismember this k v)))
  (smove [this k d m]
    (d/->deferred (.smove this k d m)))
  (smembers [this k]
    (d/chain (d/->deferred (.smembers this k))
             #(into #{} %)))
  (spop
    ([this k]
     (d/->deferred (.spop this k)))
    ([this k ^long c]
     (d/chain (d/->deferred (.spop this k c))
              #(into #{} %))))
  (srandmember
    ([this k]
     (d/->deferred (.srandmember this k)))
    ([this k ^long c]
     (d/chain (d/->deferred (.srandmember this k c))
              #(into #{} %))))
  (srem [this k m]
    (d/->deferred (.srem this k ^objects (into-array Object [m]))))
  (sunion [this ks]
    (d/chain (d/->deferred (.sunion this ^objects (into-array Object ks)))
             #(into #{} %)))
  (sunionstore [this d ks]
    (d/->deferred (.sunionstore this d ^objects (into-array Object ks))))
  (sscan
    ([this k]
     (d/->deferred (.sscan this k)))
    ([this k ^ScanCursor c]
     (d/->deferred (.sscan this k c)))
    ([this k ^ScanCursor c args]
     (d/->deferred (.sscan this k c))))

  SortedSetCommands
  (zadd
    ([this k ^double s m]
     (d/->deferred (.zadd this k s m)))
    ([this k opt ^Double s m]
     (d/->deferred (.zadd this k (zadd-args opt) s m))))
  (mzadd
    ([this k sms]
     (d/->deferred
      (.zadd this k ^objects (into-array Object (mapcat identity sms)))))
    ([this k opt sms]
     (d/->deferred
      (.zadd this k (zadd-args opt) ^objects (into-array Object (mapcat identity sms))))))
  (zaddincr [this k ^double s m]
    (d/->deferred (.zaddincr this k s m)))
  (zcard [this k]
    (d/->deferred (.zcard this k)))
  (zcount [this k ^double min ^double max]
    (d/->deferred (.zcount this k min max)))
  (zincrby [this k ^double a m]
    (d/->deferred (.zincrby this k a m)))
  (zinterstore
    ([this d ^objects ks]
     (d/->deferred (.zinterstore this d ks)))
    ([this d ^ZStoreArgs args ^objects ks]
     (d/->deferred (.zinterstore this d args ks))))
  (zrange [this k ^long s ^long e]
    (d/chain (d/->deferred (.zrange this k s e))
             #(into [] %)))
  (zrange-withscores [this k ^long s ^long e]
    (d/chain (d/->deferred (.zrangeWithScores this k s e))
             #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
             #(into [] %)))
  (zrangebyscore
    ([this k ^double min ^double max]
     (d/chain (d/->deferred (.zrangebyscore this k min max))
              #(into [] %)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (d/chain (d/->deferred (.zrangebyscore this k min max o c))
              #(into [] %))))
  (zrangebyscore-withscores
    ([this k ^double min ^double max]
     (d/chain (d/->deferred (.zrangebyscoreWithScores this k min max))
              #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
              #(into [] %)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (d/chain (d/->deferred (.zrangebyscoreWithScores this k min max o c))
              #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
              #(into [] %))))
  (zrank [this k m]
    (d/->deferred (.zrank this k m)))
  (zrem [this k m]
    (d/->deferred (.zrem this k ^objects (into-array Object [m]))))
  (mzrem [this k ms]
    (d/->deferred (.zrem this k ^objects (into-array Object ms))))
  (zremrangebyrank [this k ^long s ^long e]
    (d/->deferred (.zremrangebyrank this k s e)))
  (zremrangebyscore [this k ^Double min ^Double max]
    (d/->deferred (.zremrangebyscore this k min max)))
  (zrevrange [this k ^long s ^long e]
    (d/chain (d/->deferred (.zrevrange this k s e))
             #(into [] %)))
  (zrevrange-withscores [this k ^long s ^long e]
    (d/chain (d/->deferred (.zrevrangeWithScores this k s e))
             #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
             #(into [] %)))
  (zrevrangebyscore
    ([this k ^double min ^double max]
     (d/chain (d/->deferred (.zrevrangebyscore this k min max))
              #(into [] %)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (d/chain (d/->deferred (.zrevrangebyscore this k min max o c))
              #(into [] %))))
  (zrevrangebyscore-withscores
    ([this k ^double min ^double max]
     (d/chain (d/->deferred (.zrevrangebyscoreWithScores this k min max))
             #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
             #(into [] %)))
    ([this k ^Double min ^Double max ^Long o ^Long c]
     (d/chain (d/->deferred (.zrevrangebyscoreWithScores this k min max o c))
             #(map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]) %)
             #(into [] %))))
  (zrevrank [this k m]
    (d/->deferred (.zrevrank this k m)))
  (zscore [this k m]
    (d/->deferred (.zscore this k m)))
  (zunionstore
    ([this d ks]
     (d/->deferred (.zunionstore this d ^objects (into-array Object ks))))
    ([this d ^ZStoreArgs args ks]
     (d/->deferred (.zunionstore this d args ^objects (into-array Object ks)))))
  (zscan
    ([this k]
     (d/->deferred (.zscan this k)))
    ([this k ^ScanCursor c]
     (d/->deferred (.zscan this k c)))
    ([this k ^ScanCursor c ^ScanArgs args]
     (d/->deferred (.zscan this c args))))
  (zlexcount [this k ^String min ^String max]
    (d/->deferred (.zlexcount this k min max)))
  (zremrangebylex [this k ^String min ^String max]
    (d/->deferred (.zremrangebylex this k min max)))
  (zrangebylex
    ([this k ^String min ^String max]
     (d/chain (d/->deferred (.zrangebylex this k min max))
              #(into [] %)))
    ([this k ^String min ^String max ^Long o ^Long c]
     (d/chain (d/deferred (.zrangebylex this k min max o c))
              #(into [] %))))

  ScriptingCommands
  (eval
    ([this ^String script t ks]
     (d/->deferred
      (.eval this script (output-type t) ^objects (into-array Object ks))))
    ([this ^String script t ks vs]
     (d/->deferred
      (.eval this script (output-type t)
             ^objects (into-array Object ks)
             ^objects (into-array Object vs)))))
  (evalsha
    ([this ^String digest t ks]
     (d/->deferred
      (.evalsha this digest (output-type t) ^objects (into-array Object ks))))
    ([this ^String digest t ks vs]
     (d/->deferred
      (.evalsha this digest (output-type t)
                ^objects (into-array Object ks)
                ^objects (into-array Object vs)))))
  (script-exists? [this digests]
    (d/->deferred
     (.scriptExists this ^"[Ljava.lang.String;" (into-array String digests))))
  (script-flush [this]
    (d/->deferred (.scriptFlush this)))
  (script-kill [this]
    (d/->deferred (.scriptKill this)))
  (script-load [this ^String script]
    (d/->deferred (.scriptLoad this script)))
  (digest [this ^String script]
    (.digest this script))

  ServerCommands
  (bgrewriteaof [this]
    (d/->deferred (.bgrewriteaof this)))
  (bgsave [this]
    (d/->deferred (.bgsave this)))
  (client-getname [this]
    (d/->deferred (.clientGetname this)))
  (client-setname [this name]
    (d/->deferred (.clientSetname this name)))
  (client-kill [this addr-or-args]
    (if (instance? KillArgs addr-or-args)
      (d/->deferred (.clientKill this ^KillArgs addr-or-args))
      (d/->deferred (.clientKill this ^String addr-or-args))))
  (client-pause [this ^long timeout-ms]
    (d/->deferred (.clientPause this timeout-ms)))
  (client-list [this]
    (d/->deferred (.clientList this)))
  (command [this]
    (d/chain (d/->deferred (.command this))
             #(into [] %)))
  (command-info [this commands]
    (d/chain (d/->deferred
              (.commandInfo this ^"[Ljava.lang.String;" (into-array String commands)))
             #(into (empty commands) %)))
  (command-count [this]
    (d/->deferred (.commandCount this)))
  (config-get [this ^String param]
    (d/chain (d/->deferred (.configGet this param))
             #(into [] %)))
  (config-resetstat [this]
    (d/->deferred (.configResetstat this)))
  (config-rewrite [this]
    (d/->deferred (.configRewrite this)))
  (config-set [this ^String param ^String val]
    (d/->deferred (.configSet this param val)))
  (dbsize [this]
    (d/->deferred (.dbsize this)))
  (debug-crash-recov [this ^long delay-ms]
    (d/->deferred (.debugCrashAndRecover this delay-ms)))
  (debug-htstats [this ^Integer db]
    (d/->deferred (.debugHtstats this db)))
  (debug-object [this key]
    (d/->deferred (.debugObject this key)))
  (debug-oom [this]
    (d/->deferred (.debugOom this)))
  (debug-segfault [this]
    (d/->deferred (.debugSegfault this)))
  (debug-reload [this]
    (d/->deferred (.debugReload this)))
  (debug-restart [this ^long delay-ms]
    (d/->deferred (.debugRestart this delay-ms)))
  (debug-sds-len [this key]
    (d/->deferred (.debugSdslen this key)))
  (flushall [this]
    (d/->deferred (.flushall this)))
  (flushall-async [this]
    (d/->deferred (.flushallAsync this)))
  (flushdb [this]
    (d/->deferred (.flushdb this)))
  (flushdb-async [this]
    (d/->deferred (.flushdbAsync this)))
  (info
    ([this]
     (d/->deferred (.info this)))
    ([this ^String section]
     (d/->deferred (.info this section))))
  (lastsave [this]
    (d/->deferred (.lastsave this)))
  (save [this]
    (d/->deferred (.save this)))
  (shutdown [this save?]
    (d/->deferred (.shutdown this save?)))
  (slaveof [this ^String host ^Integer port]
    (d/->deferred (.slaveof this host port)))
  (slaveof-no-one [this]
    (d/->deferred (.slaveofNoOne this)))
  (slowlog-get
    ([this]
     (d/chain (d/->deferred (.slowlogGet this))
              #(into [] %)))
    ([this ^Integer count]
     (into [] (.slowlogGet this count))))
  (slowlog-len [this]
    (d/->deferred (.slowlogLen this)))
  (slowlog-reset [this]
    (d/->deferred (.slowlogReset this)))
  (time [this]
    (d/chain (d/->deferred (.time this))
             #(into [] %)))

  HLLCommands
  (pfadd [this key val]
    (d/->deferred (.pfadd this key ^objects (into-array Object [val]))))
  (mpfadd [this key vals]
    (d/->deferred (.pfadd this key ^objects (into-array Object vals))))
  (pfmerge [this dest keys]
    (d/->deferred (.pfmerge this dest ^objects (into-array Object keys))))
  (pfcount [this key]
    (d/->deferred (.pfcount this ^objects (into-array Object [key]))))
  (mpfcount [this keys]
    (d/->deferred (.pfcount this ^objects (into-array Object keys))))

  GeoCommands
  (geoadd
    ([this key ^Double long ^Double lat member]
     (d/->deferred
      (.geoadd this key long lat member)))
    ([this key lng-lat-members]
     (d/->deferred
      (.geoadd this key ^objects (into-array Object (mapcat identity lng-lat-members))))))
  (geohash [this key member]
    (d/chain (d/->deferred (.geohash this key ^objects (into-array Object [member])))
             #(.getValue ^Value (first %))))
  (mgeohash [this key members]
    (d/chain (d/->deferred (.geohash this key ^objects (into-array Object members)))
             #(map (fn [^Value v] (.getValue v)) %)
             #(into [] %)))
  (georadius
    ([this key ^Double long ^Double lat ^Double dist unit]
     (d/chain (d/->deferred (.georadius this key long lat dist (->unit unit)))
              #(into #{} %)))
    ([this key ^Double long ^Double lat ^Double dist unit args]
     (condp instance? args
       GeoArgs
       (d/chain (d/->deferred
                 (.georadius this key long lat dist (->unit unit) ^GeoArgs args))
                #(map (fn [^GeoWithin g]
                        (if-not g
                          nil
                          (cond-> {:member (.getMember g)}
                            (.getDistance g) (assoc :distance (.getDistance g))
                            (.getGeohash g) (assoc :geohash (.getGeohash g))
                            (.getCoordinates g)
                            (assoc :coordinates
                                   {:x (.getX ^GeoCoordinates (.getCoordinates g))
                                    :y (.getY ^GeoCoordinates (.getCoordinates g))}))))
                      %)
                #(into [] %))
       GeoRadiusStoreArgs
       (d/->deferred
        (.georadius this key long lat dist (->unit unit) ^GeoRadiusStoreArgs args))
       (throw (ex-info "Invalid Args" {:args (class args)
                                       :valids #{GeoArgs GeoRadiusStoreArgs}})))))
  (georadiusbymember
    ([this key member ^Double dist unit]
     (d/chain (d/->deferred (.georadiusbymember this key member dist (->unit unit)))
              #(into #{} %)))
    ([this key member ^Double dist unit args]
     (condp instance? args
       GeoArgs
       (d/chain (d/->deferred
                 (.georadiusbymember this key member dist (->unit unit) ^GeoArgs args))
                #(map (fn [^GeoWithin g]
                        (if-not g
                          nil
                          (cond-> {:member (.getMember g)}
                            (.getDistance g) (assoc :distance (.getDistance g))
                            (.getGeohash g) (assoc :geohash (.getGeohash g))
                            (.getCoordinates g)
                            (assoc :coordinates
                                   {:x (.getX ^GeoCoordinates (.getCoordinates g))
                                    :y (.getY ^GeoCoordinates (.getCoordinates g))}))))
                      %)
                #(into [] %))
       GeoRadiusStoreArgs
       (d/->deferred
        (.georadiusbymember this key member dist (->unit unit) ^GeoRadiusStoreArgs args))
       (throw (ex-info "Invalid Args" {:args (class args)
                                       :valids #{GeoArgs GeoRadiusStoreArgs}})))))
  (geopos [this key member]
    (d/chain (d/->deferred (.geopos this key ^objects (into-array Object [member])))
             #(map (fn [^GeoCoordinates c]
                     (if-not c nil {:x (.getX c) :y (.getY c)}))
                   %)
             #(first %)))
  (mgeopos [this key members]
    (d/chain (d/->deferred (.geopos this key ^objects (into-array Object members)))
             #(map (fn [^GeoCoordinates c]
                     (if-not c nil {:x (.getX c) :y (.getY c)}))
                   %)
             #(into [] %)))
  (geodist [this key from to unit]
    (d/->deferred (.geodist this key from to (->unit unit)))))
