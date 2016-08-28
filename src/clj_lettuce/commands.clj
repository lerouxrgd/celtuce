(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set keys sort type])
  (:require 
   [potemkin :refer [import-vars]]
   [clj-lettuce.util.scan]
   [clj-lettuce.util.migrate]))

(import-vars [clj-lettuce.util.scan 
              scan-cursor scan-args scan-res scan-seq])
(import-vars [clj-lettuce.util.migrate 
              migrate-args])

(defprotocol HashCommands
  "Redis Hash Commands"
  (hdel         [this k f]   "Delete one hash field")
  (hexists      [this k f]   "Determine if a hash field exists")
  (hget         [this k f]   "Get the value of a hash field")
  (hincrby      [this k f a] "Increment the value of a hash field by long")
  (hincrbyfloat [this k f a] "Increment the value of a hash field by double")
  (hgetall      [this k]     "Get all the fields and values in a hash")
  (hkeys        [this k]     "Get all the fields in a hash")
  (hlen         [this k]     "Get the number of fields in a hash")
  (hmdel        [this k fs]  "Delete multiple hash fields")
  (hmget        [this k fs]  "Get the values of all the given hash fields")
  (hmset        [this k m]   "Set multiple hash fields to multiple values")
  (hscan        [this k] [this k c] [this k c args]
                             "Incrementally iterate hash fields and associated values")
  (hset         [this k f v] "Set the string value of a hash field")
  (hsetnx       [this k f v] "Set the value of a hash field, only if it doesn't exist")
  (hstrlen      [this k f]   "Get the string length of the field value in a hash")
  (hvals        [this k]     "Get all the values in a hash"))

(defprotocol KeysCommands
  "Redis Keys Commands"
  (del [this k])
  (unlink [this k])
  (dump [this k])
  (exists [this k])
  (expire [this k sec])
  (expireat [this k ts-sec])
  (keys [this pattern])
  (mdel [this ks])
  (mexists [this ks])
  (migrate [this h p db ms args])
  (move [this k db]) ;; TODO ???
  (mtouch [this ks])
  (munlink [this ks])
  (object [this])
  (persist [this k])
  (pexpire [this k ms])
  (pexpireat [this k ts-ms])
  (pttl [this k])
  (randomkey [this])
  (rename [this k1 k2])
  (renamenx [this k1 k2])
  (restore [this k ttl v])
  (sort [this k] [this k args])
  (sortstore [this k1 args k2])
  (touch [this k])
  (ttl [this k])
  (type [this k])
  (scan [this] [this c] [this c args]))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (get  [this k]   "Get the value of a key")
  (set  [this k v] "Set the string value of a key")
  (mget [this ks]  "Get the values of all the given keys"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))
