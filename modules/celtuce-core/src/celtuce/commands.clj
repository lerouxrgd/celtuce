(ns celtuce.commands
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require 
   [potemkin :refer [import-vars]]
   [celtuce.args.migrate]
   [celtuce.args.sort]
   [celtuce.args.bitfield]
   [celtuce.args.set]
   [celtuce.args.zset]
   [celtuce.args.kill]
   [celtuce.args.geo]
   [celtuce.scan]))

(import-vars [celtuce.args.migrate  migrate-args])
(import-vars [celtuce.args.sort     sort-args])
(import-vars [celtuce.args.bitfield bitfield-args])
(import-vars [celtuce.args.set      set-args])
(import-vars [celtuce.args.zset     zstore-args])
(import-vars [celtuce.args.kill     kill-args])
(import-vars [celtuce.args.geo      geo-args georadius-store-args])
(import-vars [celtuce.scan          scan-cursor scan-args scan-res chunked-scan-seq
                                    scan-seq hscan-seq zscan-seq sscan-seq])

(defprotocol HashCommands
  "Redis Hash Commands"
  (hdel         [this key field]
                "Delete one hash field")
  (hexists      [this key field]
                "Determine if a hash field exists")
  (hget         [this key field]
                "Get the value of a hash field")
  (hincrby      [this key field amount]
                "Increment the value of a hash field by long")
  (hincrbyfloat [this key field amount]
                "Increment the value of a hash field by double")
  (hgetall      [this key]
                "Get all the fields and values in a hash")
  (hkeys        [this key]
                "Get all the fields in a hash")
  (hlen         [this key]
                "Get the number of fields in a hash")
  (hmdel        [this key fields]
                "Delete multiple hash fields")
  (hmget        [this key fields]
                "Get the values of all the given hash fields")
  (hmset        [this key map]
                "Set multiple hash fields to multiple values (map)")
  (hscan        [this key] [this key cursor] [this key cursor args]
                "Incrementally iterate hash fields and associated values")
  (hset         [this key field val]
                "Set the string value of a hash field")
  (hsetnx       [this key field val]
                "Set the value of a hash field, only if it doesn't exist")
  (hstrlen      [this key field]
                "Get the string length of the field value in a hash")
  (hvals        [this key]
                "Get all the values in a hash"))

(defprotocol KeyCommands
  "Redis Key Commands"
  (del          [this key]
                "Delete one key")
  (mdel         [this keys]
                "Delete multiple keys")
  (unlink       [this key]
                "Unlink one key (non blocking DEL)")
  (munlink      [this keys]
                "Unlink multiple keys (non blocking DEL)")
  (dump         [this key]
                "Serialized version of the value stored at the key")
  (exists       [this key]
                "Determine whether key exists")
  (expire       [this key sec]
                "Set a key's time to live in seconds")
  (expireat     [this key ts-sec]
                "Set the expiration for a key as a UNIX timestamp")
  (keys         [this pattern]
                "Find all keys matching the given pattern")
  (mexists      [this keys]
                "Determine how many keys exist")
  (migrate      [this host port db timeout-ms args] 
                "Transfer a key from a Redis instance to another one")
  (move         [this key db]
                "Move a key to another database")
  (obj-encoding [this key]
                "Internal representation used to store the key's value")
  (obj-idletime [this key]
                "Number of sec the key's value is idle (no read/write)")
  (obj-refcount [this key]
                "Number of references of the key's value")
  (persist      [this key]
                "Remove the expiration from a key")
  (pexpire      [this key ttl-ms]
                "Set a key's time to live in milliseconds")
  (pexpireat    [this key ts-ms]
                "Set the expiration for a key as a UNIX timestamp in ms")
  (pttl         [this key]
                "Get the time to live for a key in milliseconds")
  (randomkey    [this]
                "Return a random key from the keyspace")
  (rename       [this key1 key2]
                "Rename a key")
  (renamenx     [this key1 key2]
                "Rename a key, only if the new key does not exist")
  (restore      [this key ttl val]
                "Create a key using a serialized value obtained by DUMP")
  (sort         [this key] [this key args]
                "Sort the elements in a list, set or sorted set")
  (sort-store   [this key args dest]
                "Sort and store the result in destination key")
  (touch        [this key]
                "Touch one key. Sets the key last accessed time")
  (mtouch       [this keys]
                "Touch multiple keys. Sets the keys last accessed time")
  (ttl          [this key]
                "Get the time to live for a key")
  (type         [this key]
                "Determine the type stored at key")
  (scan         [this] [this cursor] [this cursor args] 
                "Incrementally iterate the keys space"))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (append      [this key val]
               "Append a value to a key")
  (bitcount    [this key] [this key start end]
               "Count the bits set to 1 in a string")
  (bitfield    [this key args]
               "Execute BITFIELD with its subcommands")
  (bitop-and   [this dest keys]
               "Perform bitwise AND between strings")
  (bitop-not   [this dest key]
               "Perform bitwise NOT between strings")
  (bitop-or    [this dest keys]
               "Perform bitwise OR between strings")
  (bitop-xor   [this dest keys]
               "Perform bitwise XOR between strings")
  (bitpos      [this key state] [this key state start end]
               "Find first bit set or clear in a string")
  (decr        [this key]
               "Decrement the integer value of a key by one")
  (decrby      [this key amount]
               "Decrement the integer value of a key by the given number")
  (get         [this key]
               "Get the value of a key")
  (mget        [this keys]
               "Get the values of all the given keys")
  (getbit      [this key offset]
               "Get the bit value at offset in the key's string value")
  (getrange    [this key start end]
               "Get a substring of the string stored at a key")
  (getset      [this key val]
               "Set the string value of a key and return its old value")
  (incr        [this key]
               "Increment the integer value of a key by one")
  (incrby      [this key amount]
               "Increment the integer value of a key by the given amount")
  (incrbyfloat [this key amount]
               "Increment the float value of a key by the given amount")
  (set         [this key val] [this key val args]
               "Set the string value of a key")
  (mset        [this map]
               "Set multiple keys to multiple values (map)")
  (setbit      [this key offset val]
               "Sets or clears the bit at offset in the key's string value")
  (setex       [this key ttl-sec val]
               "Set the value and expiration of a key")
  (psetex      [this key ttl-ms val]
               "Set the value and expiration in milliseconds of a key")
  (setnx       [this key val]
               "Set the value of a key, only if the key does not exist")
  (msetnx      [this map]
               "Like mset, but only if none of the keys exist")
  (setrange    [this key offset val]
               "Overwrite part of a string at key starting at the offset")
  (strlen      [this key]
               "Get the length of the value stored in a key"))

(defprotocol ListCommands
  "Redis List Commands"
  (blpop      [this timeout-sec keys]
              "Remove and get the first elem (block until there's one)")
  (brpop      [this timeout-sec keys]
              "Remove and get the last elem (block until there's one)")
  (brpoplpush [this timeout-sec src dest]
              "Pop and push to another list, return the elem (blocking)")
  (lindex     [this key idx]
              "Get an element from a list by its index")
  (linsert    [this key before? pivot val]
              "Insert an elem before or after another elem in a list")
  (llen       [this key]
              "Get the length of a list")
  (lpop       [this key]
              "Remove and get the first element in a list")
  (lpush      [this key val]
              "Prepend one value to a list")
  (mlpush     [this key vals]
              "Prepend multiple values to a list")
  (lpushx     [this key val]
              "Prepend a value to a list, only if the list exists")
  (lrange     [this key start end]
              "Get a range of elements from a list")
  (lrem       [this key count val]
              "Remove elements from a list")
  (lset       [this key idx v]
              "Set the value of an element in a list by its index")
  (ltrim      [this key start end]
              "Trim a list to the specified range")
  (rpop       [this key]
              "Remove and get the last element in a list")
  (rpoplpush  [this src dest]
              "Pop and push to another list, return the elem")
  (rpush      [this key val]
              "Append one value to a list")
  (mrpush     [this key vals]
              "Append multiple values to a list")
  (rpushx     [this key val]
              "Append a value to a list, only if the list exists"))

(defprotocol SetCommands
  "Redis Set Commands"
  (sadd        [this key member]
               "Add one member to a set")
  (msadd       [this key members]
               "Add multiple members to a set")
  (scard       [this key]
               "Get the number of members in a set")
  (sdiff       [this keys]
               "Subtract multiple sets")
  (sdiffstore  [this dest keys]
               "Subtract multiple sets and store the resulting set in a key")
  (sinter      [this keys]
               "Intersect multiple sets")
  (sinterstore [this dest keys]
               "Intersect multiple sets and store the resulting set in a key")
  (sismember   [this key val]
               "Determine if a given value is a member of a set")
  (smove       [this key dest member]
               "Move a member from one set to another")
  (smembers    [this key]
               "Get all the members in a set")
  (spop        [this key] [this key count]
               "Remove and return one or multiple random members from a set")
  (srandmember [this key] [this key count]
               "Get one or multiple random members from a set")
  (srem        [this key member]
               "Remove one member from a set")
  (msrem       [this key members]
               "Remove multiple members from a set")
  (sunion      [this keys]
               "Add multiple sets")
  (sunionstore [this dest keys]
               "Add multiple sets and store the resulting set in a key")
  (sscan       [this key] [this key cursor] [this key cursor args]
               "Incrementally iterate set elements"))

(defprotocol SortedSetCommands
  "Redis Sorted Set Commands"
  (zadd             [this key score member] [this key args score member]
                    "Add one member to a sorted set (update score if exists)")
  (mzadd            [this key scored-members] [this key args scored-members]
                    "Add multiple members to a sorted set (update scores if exist)")
  (zaddincr         [this key score member]
                    "Increment the score of a member in a sorted set")
  (zcard            [this key]
                    "Get the number of members in a sorted set")
  (zcount           [this key min max]
                    "Count the members in a sorted set with scores within [min max]")
  (zincrby          [this key amount member]
                    "Increment the score of a member in a sorted set")
  (zinterstore      [this dest keys] [this dest args keys]
                    "Intersect multiple sorted sets ks, store the result in a new key d")
  (zrange           [this key start end]
                    "Return a range of members in a sorted set, by index (scores asc)")
  (zrangebyscore    [this key min max] [this key min max offset count]
                    "Return a range of members in a sorted set, by score (scores asc)")
  (zrank            [this key member]
                    "Determine the index of a member in a sorted set (score asc)")
  (zrem             [this key member]
                    "Remove one member from a sorted set")
  (mzrem            [this key members]
                    "Remove multiple members from a sorted set")
  (zremrangebyrank  [this key start end]
                    "Remove all members in a sorted set within the given indexes")
  (zremrangebyscore [this key min max]
                    "Remove all members in a sorted set within the given scores")
  (zrevrange        [this key start end]
                    "Return a range of members in a sorted set, by index (scores desc)")
  (zrevrangebyscore [this key min max] [this key min max offset count]
                    "Return a range of members in a sorted set, by score (scores desc)")
  (zrevrank         [this key member]
                    "Determine the index of a member in a sorted set (score desc)")
  (zscan            [this key] [this key cursor] [this key cursor args]
                    "Incrementally iterate sorted sets elements and associated scores")
  (zscore           [this key member]
                    "Get the score associated with the given member in a sorted set.")
  (zunionstore      [this dest keys] [this dest args keys]
                    "Add multiple sorted sets ks, store the result in a new key d")
  (zlexcount        [this key min max]
                    "Count the members in a sorted set in a given lexicographical range")
  (zrangebylex      [this key min max] [this key min max offset count]
                    "Return a range of members in a sorted set, by lexicographical range")
  (zremrangebylex   [this key min max]
                    "Remove all members in a sorted set in a given lexicographical range")
  ;; with scores range commands
  (zrange-withscores           [this key start end])
  (zrangebyscore-withscores    [this key min max] [this key min max offset count])
  (zrevrange-withscores        [this key start end])
  (zrevrangebyscore-withscores [this key min max] [this key min max offset count]))

(defprotocol ScriptingCommands
  "Redis Scripting Commands (Lua 5.1)"
  (eval           [this script type keys] [this script type keys vals]
                  "Execute a Lua script server side")
  (evalsha        [this digest type keys] [this digest type keys vals]
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
  (bgrewriteaof      [this]
                     "Asynchronously rewrite the append-only file")
  (bgsave            [this]
                     "Asynchronously save the dataset to disk")
  (client-getname    [this]
                     "Get the current connection name")
  (client-setname    [this name]
                     "Set the current connection name")
  (client-kill       [this addr-or-args]
                     "Kill the connection of a client identified by ip:port, or args")
  (client-pause      [this timeout-ms]
                     "Stop processing commands from clients for some time")
  (client-list       [this]
                     "Get the list of client connections")
  (command           [this]
                     "Return an array reply of details about all Redis commands")
  (command-info      [this commands]
                     "Return an array reply of details about the requested commands")
  (command-count     [this]
                     "Get total number of Redis commands")
  (config-get        [this param]
                     "Get the value of a configuration parameter")
  (config-resetstat  [this]
                     "Reset the stats returned by INFO")
  (config-rewrite    [this]
                     "Rewrite the configuration file with the in memory configuration")
  (config-set        [this param val]
                     "Set a configuration parameter to the given value")
  (dbsize            [this]
                     "Return the number of keys in the selected database")
  (debug-crash-recov [this delay-ms]
                     "Crash and recover")
  (debug-htstats     [this db]
                     "Get debugging information about the internal hash-table state")
  (debug-object      [this key]
                     "Get debugging information about a key")
  (debug-oom         [this]
                     "Make the server crash: Out of memory")
  (debug-segfault    [this]
                     "Make the server crash: Invalid pointer access")
  (debug-reload      [this]
                     "Save RDB, clear the database and reload RDB")
  (debug-restart     [this delay-ms]
                     "Restart the server gracefully")
  (debug-sds-len     [this key]
                     "Get debugging information about the internal SDS length")
  (flushall          [this]
                     "Remove all keys from all databases")
  (flushall-async    [this]
                     "Remove all keys asynchronously from all databases")
  (flushdb           [this]
                     "Remove all keys from the current database")
  (flushdb-async     [this]
                     "Remove all keys asynchronously from the current database")
  (info              [this] [this section]
                     "Get information and statistics about the server")
  (lastsave          [this]
                     "Get the UNIX time stamp of the last successful save to disk")
  (save              [this]
                     "Synchronously save the dataset to disk")
  (shutdown          [this save?]
                     "Synchronously save the dataset to disk and shutdown the server")
  (slaveof           [this host port]
                     "Make the server a slave of another server, or promote it as master")
  (slaveof-no-one    [this]
                     "Promote server as master")
  (slowlog-get       [this] [this count]
                     "Read the slow log")
  (slowlog-len       [this]
                     "Obtaining the current length of the slow log")
  (slowlog-reset     [this]
                     "Resetting the slow log")
  (time              [this]
                     "Return the current server time"))

(defprotocol HLLCommands
  "Redis HLL Commands"
  (pfadd    [this key val]
            "Add the specified element to the specified HyperLogLog")
  (mpfadd   [this key vals]
            "Add the specified elements to the specified HyperLogLog")
  (pfmerge  [this dest keys]
            "Merge N different HyperLogLogs into a single one")
  (pfcount  [this key]
            "Return the approximated cardinality of the set (HyperLogLog) at key")
  (mpfcount [this keys]
            "Return the approximated cardinality of the sets (HyperLogLog) at keys"))

(defprotocol GeoCommands
  "Redis Geo Commands"
  (geoadd            [this key long lat member] [this key lng-lat-members]
                     "Single or multiple geo add")
  (geohash           [this key member]
                     "Retrieve Geohash of a member of a geospatial index")
  (mgeohash          [this key members]
                     "Retrieve Geohash of multiple members of a geospatial index")
  (georadius         [this key long lat dist unit] [this key long lat dist unit args]
                     "Retrieve members selected by dist with the center of long last,
                      Perform a georadius query and store the results in a zset")
  (georadiusbymember [this key member dist unit] [this key member dist unit args]
                     "Retrieve members selected by dist with the center of member,
                      Perform a georadiusbymember query and store the results in a zset")
  (geopos            [this key member]
                     "Get geo coordinates for the member")
  (mgeopos           [this key members]
                     "Get geo coordinates for the members")
  (geodist           [this key from to unit]
                     "Retrieve distance between points from and to"))

(defprotocol PubSubCommands
  "Redis PubSub Commands"
  (publish         [this channel message]
                   "Post a message to a channel")
  (subscribe       [this channel]
                   "Listen for messages published to channel")
  (unsubscribe     [this channel]
                   "Stop listening for messages posted to channel")
  (msubscribe      [this channels]
                   "Listen for messages published to channels")
  (munsubscribe    [this channels]
                   "Stop listening for messages posted to channels")
  (psubscribe      [this pattern]
                   "Listen for messages published to channels matching pattern")
  (punsubscribe    [this pattern]
                   "Stop listening for messages posted to channels matching pattern")
  (mpsubscribe     [this pattern]
                   "Listen for messages published to channels matching patterns")
  (mpunsubscribe   [this pattern]
                   "Stop listening for messages posted to channels matching patterns")
  (pubsub-channels [this] [this channel]
                   "Lists the currently *active channels*")
  (pubsub-numsub   [this channel]
                   "Returns the number of subscribers for the specified channel")
  (pubsub-numpat   [this]
                   "Returns the number of subscriptions to patterns"))

(defprotocol PubSubListener
  "Protocol for redis pub/sub listeners"
  (message       [this channel message] [this pattern channel message]
                 "Message received from a channel (or pattern) subscription")
  (subscribed    [this channel count]
                 "Subscribed to a channel")
  (unsubscribed  [this channel count]
                 "Unsubscribed from a channel")
  (psubscribed   [this pattern count]
                 "Subscribed to a pattern")
  (punsubscribed [this pattern count]
                 "Unsubscribed from a pattern"))

