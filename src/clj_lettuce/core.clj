(ns clj-lettuce.core
  (:require [clj-lettuce.commands :as redis]
            [clj-lettuce.cluster :as cluster]))

#_
(let [rcli (-> "redis://localhost:30001" cluster/redis-cli)
      conn (cluster/stateful-conn rcli)
      cmds (redis/mk-commands :cluster-sync conn)]
  (redis/set cmds "foo" "bar")
  (redis/set cmds "foofoo" "barbar")
  (println (redis/mget cmds ["foo" "foofoo"]))
  (cluster/close-conn conn)
  (cluster/shutdown rcli))
