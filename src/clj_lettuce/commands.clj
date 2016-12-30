(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set keys sort type eval])
  (:require 
   [potemkin :refer [import-vars]]
   [clj-lettuce.args.scan]
   [clj-lettuce.args.migrate]
   [clj-lettuce.args.sort]
   [clj-lettuce.args.bitfield]
   [clj-lettuce.args.set]
   [clj-lettuce.args.zset]))

(import-vars [clj-lettuce.args.scan     scan-cursor scan-args scan-res scan-seq])
(import-vars [clj-lettuce.args.migrate  migrate-args])
(import-vars [clj-lettuce.args.sort     sort-args])
(import-vars [clj-lettuce.args.bitfield bitfield-args])
(import-vars [clj-lettuce.args.set      set-args])
(import-vars [clj-lettuce.args.zset     zstore-args])

(defprotocol HashCommands
  "Redis Hash Commands"
  (hdel         [this k f]
                "Delete one hash field")
  (hexists      [this k f]
                "Determine if a hash field exists")
  (hget         [this k f]
                "Get the value of a hash field")
  (hincrby      [this k f a]
                "Increment the value of a hash field by long")
  (hincrbyfloat [this k f a]
                "Increment the value of a hash field by double")
  (hgetall      [this k]
                "Get all the fields and values in a hash")
  (hkeys        [this k]
                "Get all the fields in a hash")
  (hlen         [this k]
                "Get the number of fields in a hash")
  (hmdel        [this k fs]
                "Delete multiple hash fields")
  (hmget        [this k fs]
                "Get the values of all the given hash fields")
  (hmset        [this k m]
                "Set multiple hash fields to multiple values")
  (hscan        [this k] [this k c] [this k c args]
                "Incrementally iterate hash fields and associated values")
  (hset         [this k f v]
                "Set the string value of a hash field")
  (hsetnx       [this k f v]
                "Set the value of a hash field, only if it doesn't exist")
  (hstrlen      [this k f]
                "Get the string length of the field value in a hash")
  (hvals        [this k]
                "Get all the values in a hash"))

(defprotocol KeyCommands
  "Redis Key Commands"
  (del          [this k]
                "Delete one key")
  (mdel         [this ks]
                "Delete multiple keys")
  (unlink       [this k]
                "Unlink one key (non blocking DEL)")
  (munlink      [this ks]
                "Unlink multiple keys (non blocking DEL)")
  (dump         [this k]
                "Serialized version of the value stored at the key")
  (exists       [this k]
                "Determine whether key exists")
  (expire       [this k sec]
                "Set a key's time to live in seconds")
  (expireat     [this k ts-sec]
                "Set the expiration for a key as a UNIX timestamp")
  (keys         [this pattern]
                "Find all keys matching the given pattern")
  (mexists      [this ks]
                "Determine how many keys exist")
  (migrate      [this h p db ms args] 
                "Transfer a key from a Redis instance to another one")
  (move         [this k db]
                "Move a key to another database")
  (obj-encoding [this k]
                "Internal representation used to store the key's value")
  (obj-idletime [this k]
                "Number of sec the key's value is idle (no read/write)")
  (obj-refcount [this k]
                "Number of references of the key's value")
  (persist      [this k]
                "Remove the expiration from a key")
  (pexpire      [this k ms]
                "Set a key's time to live in milliseconds")
  (pexpireat    [this k ts-ms]
                "Set the expiration for a key as a UNIX timestamp in ms")
  (pttl         [this k]
                "Get the time to live for a key in milliseconds")
  (randomkey    [this]
                "Return a random key from the keyspace")
  (rename       [this k1 k2]
                "Rename a key")
  (renamenx     [this k1 k2]
                "Rename a key, only if the new key does not exist")
  (restore      [this k ttl v]
                "Create a key using a serialized value obtained by DUMP")
  (sort         [this k] [this k args]
                "Sort the elements in a list, set or sorted set")
  (sort-store   [this k args d]
                "Sort and store the result in destination key")
  (touch        [this k]
                "Touch one key. Sets the key last accessed time")
  (mtouch       [this ks]
                "Touch multiple keys. Sets the keys last accessed time")
  (ttl          [this k]
                "Get the time to live for a key")
  (type         [this k]
                "Determine the type stored at key")
  (scan         [this] [this c] [this c args] 
                "Incrementally iterate the keys space"))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (append      [this k v]
               "Append a value to a key")
  (bitcount    [this k] [this k s e]
               "Count the bits set to 1 in a string")
  (bitfield    [this k args]
               "Execute BITFIELD with its subcommands")
  (bitop-and   [this d ks]
               "Perform bitwise AND between strings")
  (bitop-not   [this d k]
               "Perform bitwise NOT between strings")
  (bitop-or    [this d ks]
               "Perform bitwise OR between strings")
  (bitop-xor   [this d ks]
               "Perform bitwise XOR between strings")
  (bitpos      [this k state] [this k state s e]
               "Find first bit set or clear in a string")
  (decr        [this k]
               "Decrement the integer value of a key by one")
  (decrby      [this k a]
               "Decrement the integer value of a key by the given number")
  (get         [this k]
               "Get the value of a key")
  (mget        [this ks]
               "Get the values of all the given keys")
  (getbit      [this k o]
               "Get the bit value at offset in the key's string value")
  (getrange    [this k s e]
               "Get a substring of the string stored at a key")
  (getset      [this k v]
               "Set the string value of a key and return its old value")
  (incr        [this k]
               "Increment the integer value of a key by one")
  (incrby      [this k a]
               "Increment the integer value of a key by the given amount")
  (incrbyfloat [this k a]
               "Increment the float value of a key by the given amount")
  (set         [this k v] [this k v args]
               "Set the string value of a key")
  (mset        [this m]
               "Set multiple keys to multiple values")
  (setbit      [this k o v]
               "Sets or clears the bit at offset in the key's string value")
  (setex       [this k sec v]
               "Set the value and expiration of a key")
  (psetex      [this k ms v]
               "Set the value and expiration in milliseconds of a key")
  (setnx       [this k v]
               "Set the value of a key, only if the key does not exist")
  (msetnx      [this m]
               "Like mset, but only if none of the keys exist")
  (setrange    [this k o v]
               "Overwrite part of a string at key starting at the offset")
  (strlen      [this k]
               "Get the length of the value stored in a key"))

(defprotocol ListCommands
  "Redis List Commands"
  (blpop      [this sec ks]
              "Remove and get the first elem (block until there's one)")
  (brpop      [this sec ks]
              "Remove and get the last elem (block until there's one)")
  (brpoplpush [this sec s d]
              "Pop and push to another list, return the elem (blocking)")
  (lindex     [this k idx]
              "Get an element from a list by its index")
  (linsert    [this k b? p v]
              "Insert an elem before or after another elem in a list")
  (llen       [this k]
              "Get the length of a list")
  (lpop       [this k]
              "Remove and get the first element in a list")
  (lpush      [this k v]
              "Prepend one value to a list")
  (mlpush     [this k vs]
              "Prepend multiple values to a list")
  (lpushx     [this k v]
              "Prepend a value to a list, only if the list exists")
  (lrange     [this k s e]
              "Get a range of elements from a list")
  (lrem       [this k c v]
              "Remove elements from a list")
  (lset       [this k idx v]
              "Set the value of an element in a list by its index")
  (ltrim      [this k s e]
              "Trim a list to the specified range")
  (rpop       [this k]
              "Remove and get the last element in a list")
  (rpoplpush  [this s d]
              "Pop and push to another list, return the elem")
  (rpush      [this k v]
              "Append one value to a list")
  (mrpush     [this k vs]
              "Append multiple values to a list")
  (rpushx     [this k v]
              "Append a value to a list, only if the list exists"))

(defprotocol SetCommands
  "Redis Set Commands"
  (sadd        [this k m]
               "Add one member to a set")
  (msadd       [this k ms]
               "Add multiple members to a set")
  (scard       [this k]
               "Get the number of members in a set")
  (sdiff       [this ks]
               "Subtract multiple sets")
  (sdiffstore  [this d ks]
               "Subtract multiple sets and store the resulting set in a key")
  (sinter      [this ks]
               "Intersect multiple sets")
  (sinterstore [this d ks]
               "Intersect multiple sets and store the resulting set in a key")
  (sismember   [this k v]
               "Determine if a given value is a member of a set")
  (smove       [this k d m]
               "Move a member from one set to another")
  (smembers    [this k]
               "Get all the members in a set")
  (spop        [this k] [this k c]
               "Remove and return one or multiple random members from a set")
  (srandmember [this k] [this k c]
               "Get one or multiple random members from a set")
  (srem        [this k m]
               "Remove one member from a set")
  (msrem       [this k ms]
               "Remove multiple members from a set")
  (sunion      [this ks]
               "Add multiple sets")
  (sunionstore [this d ks]
               "Add multiple sets and store the resulting set in a key")
  (sscan       [this k] [this k c] [this k c args]
               "Incrementally iterate set elements"))

(defprotocol SortedSetCommands
  "Redis Sorted Set Commands"
  (zadd             [this k s m] [this k args s m]
                    "Add one member to a sorted set (update score if exists)")
  (mzadd            [this k sms] [this k args sms]
                    "Add multiple members to a sorted set (update scores if exist)")
  (zaddincr         [this k s m]
                    "Increment the score of a member in a sorted set")
  (zcard            [this k]
                    "Get the number of members in a sorted set")
  (zcount           [this k min max]
                    "Count the members in a sorted set with scores within [min max]")
  (zincrby          [this k a m]
                    "Increment the score of a member in a sorted set")
  (zinterstore      [this d ks] [this d args ks]
                    "Intersect multiple sorted sets ks, store the result in a new key d")
  (zrange           [this k s e]
                    "Return a range of members in a sorted set, by index (scores asc)")
  (zrangebyscore    [this k min max] [this k min max o c]
                    "Return a range of members in a sorted set, by score (scores asc)")
  (zrank            [this k m]
                    "Determine the index of a member in a sorted set (score asc)")
  (zrem             [this k m]
                    "Remove one member from a sorted set")
  (mzrem            [this k ms]
                    "Remove multiple members from a sorted set")
  (zremrangebyrank  [this k s e]
                    "Remove all members in a sorted set within the given indexes")
  (zremrangebyscore [this k min max]
                    "Remove all members in a sorted set within the given scores")
  (zrevrange        [this k s e]
                    "Return a range of members in a sorted set, by index (scores desc)")
  (zrevrangebyscore [this k min max] [this k min max o c]
                    "Return a range of members in a sorted set, by score (scores desc)")
  (zrevrank         [this k m]
                    "Determine the index of a member in a sorted set (score desc)")
  (zscan            [this k] [this k c] [this k c args]
                    "Incrementally iterate sorted sets elements and associated scores")
  (zscore           [this k m]
                    "Get the score associated with the given member in a sorted set.")
  (zunionstore      [this d ks] [this d args ks]
                    "Add multiple sorted sets ks, store the result in a new key d")
  (zlexcount        [this k min max]
                    "Count the members in a sorted set in a given lexicographical range")
  (zrangebylex      [this k min max] [this k min max o c]
                    "Return a range of members in a sorted set, by lexicographical range")
  (zremrangebylex   [this k min max]
                    "Remove all members in a sorted set in a given lexicographical range")
  ;; with scores range commands
  (zrange-withscores           [this k s e])
  (zrangebyscore-withscores    [this k min max] [this k min max o c])
  (zrevrange-withscores        [this k s e])
  (zrevrangebyscore-withscores [this k min max] [this k min max o c]))

(defprotocol ScriptingCommands
  "Redis Scripting Commands (Lua 5.1)"
  (eval           [this script type ks] [this script type ks vs]
                  "Execute a Lua script server side")
  (evalsha        [this digest type ks] [this digest type ks vs]
                  "Evaluates a script cached on the server side by its SHA1 digest")
  (script-exists? [this digests]
                  "Check existence of scripts in the script cache")
  (script-flush   [this]
                  "Remove all the scripts from the script cache")
  (script-kill    [this]
                  "Kill the script currently in execution")
  (scirpt-load    [this script]
                  "Load the specified Lua script into the script cache")
  (digest         [this script]
                  "Create a SHA1 digest from a Lua script"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))

