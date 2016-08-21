(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set]))

(defprotocol RedisConnector
  "Functions to manipulate Redis client and connections"
  (redis-cli     [this])
  (stateful-conn [this])
  (conn-open?    [this])
  (close-conn    [this])
  (shutdown      [this]))

(defmulti commands
  "Redis commands implementation, depends on the client and connection type"
  (fn [type redis-connector] type))

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
  (hscan        [this k]     "Incrementally iterate hash fields and associated values")
  (hset         [this k f v] "Set the string value of a hash field")
  (hsetnx       [this k f v] "Set the value of a hash field, only if it doesn't exist")
  (hstrlen      [this k f]   "Get the string length of the field value in a hash")
  (hvals        [this k]     "Get all the values in a hash"))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (get  [this k]   "Get the value of a key")
  (set  [this k v] "Set the string value of a key")
  (mget [this ks]  "Get the values of all the given keys"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))
