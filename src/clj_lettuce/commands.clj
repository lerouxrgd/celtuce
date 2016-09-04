(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set keys sort type])
  (:require 
   [potemkin :refer [import-vars]]
   [clj-lettuce.args.scan]
   [clj-lettuce.args.migrate]
   [clj-lettuce.args.sort]
   [clj-lettuce.args.bitfield]
   [clj-lettuce.args.set]))

(import-vars [clj-lettuce.args.scan     scan-cursor scan-args scan-res scan-seq])
(import-vars [clj-lettuce.args.migrate  migrate-args])
(import-vars [clj-lettuce.args.sort     sort-args])
(import-vars [clj-lettuce.args.bitfield bitfield-args])
(import-vars [clj-lettuce.args.set      set-args])

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

(defprotocol KeyCommands
  "Redis Key Commands"
  (del          [this k]        "Delete one key")
  (unlink       [this k]        "Unlink one key (non blocking DEL)")
  (dump         [this k]        "Serialized version of the value stored at the key")
  (exists       [this k]        "Determine whether key exists")
  (expire       [this k sec]    "Set a key's time to live in seconds")
  (expireat     [this k ts-sec] "Set the expiration for a key as a UNIX timestamp")
  (keys         [this pattern]  "Find all keys matching the given pattern")
  (mdel         [this ks]       "Delete multiple keys")
  (mexists      [this ks]       "Determine how many keys exist")
  (migrate      [this h p db ms args] 
                                "Transfer a key from a Redis instance to another one")
  (move         [this k db]     "Move a key to another database")
  (mtouch       [this ks]       "Touch multiple keys. Sets the keys last accessed time")
  (munlink      [this ks]       "Unlink multiple keys (non blocking DEL)")
  (obj-encoding [this k]        "Internal representation used to store the key's value")
  (obj-idletime [this k]        "Number of sec the key's value is idle (no read/write)")
  (obj-refcount [this k]        "Number of references of the key's value")
  (persist      [this k]        "Remove the expiration from a key")
  (pexpire      [this k ms]     "Set a key's time to live in milliseconds")
  (pexpireat    [this k ts-ms]  "Set the expiration for a key as a UNIX timestamp in ms")
  (pttl         [this k]        "Get the time to live for a key in milliseconds")
  (randomkey    [this]          "Return a random key from the keyspace")
  (rename       [this k1 k2]    "Rename a key")
  (renamenx     [this k1 k2]    "Rename a key, only if the new key does not exist")
  (restore      [this k ttl v]  "Create a key using a serialized value obtained by DUMP")
  (sort         [this k] [this k args]
                                "Sort the elements in a list, set or sorted set")
  (sort-store   [this k args d] "Sort and store the result in destination key")
  (touch        [this k]        "Touch one key. Sets the key last accessed time")
  (ttl          [this k]        "Get the time to live for a key")
  (type         [this k]        "Determine the type stored at key")
  (scan         [this] [this c] [this c args] 
                                "Incrementally iterate the keys space"))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (append      [this k v]     "Append a value to a key")
  (bitcount    [this k] [this k s e]
                              "Count the bits set to 1 in a string")
  (bitfield    [this k args]  "Execute BITFIELD with its subcommands")
  (bitop-and   [this d ks]    "Perform bitwise AND between strings")
  (bitop-not   [this d k]     "Perform bitwise NOT between strings")
  (bitop-or    [this d ks]    "Perform bitwise OR between strings")
  (bitop-xor   [this d ks]    "Perform bitwise XOR between strings")
  (bitpos      [this k state] [this k state s e]
                              "Find first bit set or clear in a string")
  (decr        [this k]       "Decrement the integer value of a key by one")
  (decrby      [this k a]     "Decrement the integer value of a key by the given number")
  (get         [this k]       "Get the value of a key")
  (getbit      [this k o]     "Get the bit value at offset in the key's string value")
  (getrange    [this k s e]   "Get a substring of the string stored at a key")
  (getset      [this k v]     "Set the string value of a key and return its old value")
  (incr        [this k]       "Increment the integer value of a key by one")
  (incrby      [this k a]     "Increment the integer value of a key by the given amount")
  (incrbyfloat [this k a]     "Increment the float value of a key by the given amount")
  (mget        [this ks]      "Get the values of all the given keys")
  (mset        [this m]       "Set multiple keys to multiple values")
  (msetnx      [this m]       "Like mset, but only if none of the keys exist")
  (set         [this k v] [this k v args]
                              "Set the string value of a key")
  (setbit      [this k o v]   "Sets or clears the bit at offset in the key's string value")
  (setex       [this k sec v] "Set the value and expiration of a key")
  (psetex      [this k ms v]  "Set the value and expiration in milliseconds of a key")
  (setnx       [this k v]     "Set the value of a key, only if the key does not exist")
  (setrange    [this k o v]   "Overwrite part of a string at key starting at the offset")
  (strlen      [this k]       "Get the length of the value stored in a key"))

(defprotocol ListCommands
  "Redis List Commands"
  (blpop      [this sec ks]   "Remove and get the first elem (block until there's one)")
  (brpop      [this sec ks]   "Remove and get the last elem (block until there's one)")
  (brpoplpush [this sec s d]  "Pop and push to another list, return the elem (blocking)")
  (lindex     [this k idx]    "Get an element from a list by its index")
  (linsert    [this k b? p v] "Insert an elem before or after another elem in a list")
  (llen       [this k]        "Get the length of a list")
  (lpop       [this k]        "Remove and get the first element in a list")
  (lpush      [this k v]      "Prepend one value to a list")
  (lpushx     [this k v]      "Prepend a value to a list, only if the list exists")
  (lrange     [this k s e]    "Get a range of elements from a list")
  (lrem       [this k c v]    "Remove elements from a list")
  (lset       [this k idx v]  "Set the value of an element in a list by its index")
  (ltrim      [this k s e]    "Trim a list to the specified range")
  (mlpush     [this k vs]     "Prepend multiple values to a list")
  (mrpush     [this k vs]     "Append multiple values to a list")
  (rpop       [this k]        "Remove and get the last element in a list")
  (rpoplpush  [this s d]      "Pop and push to another list, return the elem")
  (rpush      [this k v]      "Append one value to a list")
  (rpushx     [this k v]      "Append a value to a list, only if the list exists"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))

