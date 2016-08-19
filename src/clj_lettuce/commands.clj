(ns clj-lettuce.commands
  (:refer-clojure :exclude [get set]))

(defprotocol RedisConnector
  "Functions to manipulate Redis client and connections"
  (redis-cli [this])
  (stateful-conn [this])
  (conn-open? [this])
  (close-conn [this])
  (shutdown [this]))

(defmulti commands
  (fn [type redis-connector] type))

(defprotocol HashCommands
  "Redis Hash Commands"
  (hdel [this k fs] "")
  (hexists [this k f] "")
  (hget [this k f] "")
  (hincrby [this k f a] "")
  (hincrbyfloat [this k f a] "")
  (hgetall [this k] "")
  (hkeys [this k] "")
  (hlen [this k] "")
  (hmget [this k fs] "")
  (hmset [this k m] "")
  (hscan [this k] "")
  (hset [this k f v] "")
  (hsetnx [this k f v] "")
  (hstrlen [this k f] "")
  (hvals [this k] ""))

(defprotocol StringsCommands
  "Redis Strings Commands"
  (get [this k]   "Get the value of a key")
  (set [this k v] "Set the string value of a key")
  (mget [this ks] "Get the values of all the given keys"))

(defprotocol ServerCommands
  "Redis Server Commands"
  (flushall [this] "Remove all keys from all databases"))
