(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set keys sort type])
  (:require 
   [potemkin :refer [import-vars]]
   [clj-lettuce.util.scan]
   [clj-lettuce.util.migrate]
   [clj-lettuce.util.sort]))

(import-vars [clj-lettuce.util.scan 
              scan-cursor scan-args scan-res scan-seq])
(import-vars [clj-lettuce.util.migrate 
              migrate-args])
(import-vars [clj-lettuce.util.sort
              sort-args])

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
  (del             [this k]        "Delete one key")
  (unlink          [this k]        "Unlink one key (non blocking DEL)")
  (dump            [this k]        "Serialized version of the value stored at the key")
  (exists          [this k]        "Determine whether key exists")
  (expire          [this k sec]    "Set a key's time to live in seconds")
  (expireat        [this k ts-sec] "Set the expiration for a key as a UNIX timestamp")
  (keys            [this pattern]  "Find all keys matching the given pattern")
  (mdel            [this ks]       "Delete multiple keys")
  (mexists         [this ks]       "Determine how many keys exist")
  (migrate         [this h p db ms args] 
                                   "Transfer a key from a Redis instance to another one")
  (move            [this k db]     "Move a key to another database")
  (mtouch          [this ks]       "Touch multiple keys. Sets the keys last accessed time")
  (munlink         [this ks]       "Unlink multiple keys (non blocking DEL)")
  (object-encoding [this k]        "Internal representation used to store the key's value")
  (object-idletime [this k]        "Number of sec the key's value is idle (no read/write)")
  (object-refcount [this k]        "Number of references of the key's value")
  (persist         [this k]        "Remove the expiration from a key")
  (pexpire         [this k ms]     "Set a key's time to live in milliseconds")
  (pexpireat       [this k ts-ms]  "Set the expiration for a key as a UNIX timestamp in ms")
  (pttl            [this k]        "Get the time to live for a key in milliseconds")
  (randomkey       [this]          "Return a random key from the keyspace")
  (rename          [this k1 k2]    "Rename a key")
  (renamenx        [this k1 k2]    "Rename a key, only if the new key does not exist")
  (restore         [this k ttl v]  "Create a key using a serialized value obtained by DUMP")
  (sort            [this k] [this k args]
                                   "Sort the elements in a list, set or sorted set")
  (sort-store      [this k args d] "Sort and store the result in destination key")
  (touch           [this k]        "Touch one key. Sets the key last accessed time")
  (ttl             [this k]        "Get the time to live for a key")
  (type            [this k]        "Determine the type stored at key")
  (scan            [this] [this c] [this c args] 
                                   "Incrementally iterate the keys space"))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (get  [this k]   "Get the value of a key")
  (set  [this k v] "Set the string value of a key")
  (mget [this ks]  "Get the values of all the given keys"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))
