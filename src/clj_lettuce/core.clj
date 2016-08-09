(ns clj-lettuce.core
  (:require [clj-lettuce.commands :as redis]
            [clj-lettuce.cluster :refer [redis-cluster]]))

#_
(let [rclust (redis-cluster "redis://localhost:30001")
      cmds (redis/commands :cluster-sync rclust)]
  (redis/set cmds "foo" "bar")
  (redis/set cmds "foofoo" "barbar")
  (println (redis/mget cmds ["foo" "foofoo"]))
  (redis/close-conn rclust)
  (redis/shutdown rclust))
