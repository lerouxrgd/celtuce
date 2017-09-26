(ns celtuce.pool-test
  (:require 
   [clojure.test :refer :all]
   [celtuce.commands :as redis]
   [celtuce.connector :as conn]
   [celtuce.pool :as pool]
   [celtuce.manifold :refer [commands-manifold]]
   [manifold.deferred :as d]))

(def redis-url "redis://localhost:6379")
(def redis-conn (conn/redis-server redis-url))

(defn cleanup-fixture [test-function]
  (redis/flushall (conn/commands-sync redis-conn))
  (test-function)
  (conn/shutdown redis-conn))

(use-fixtures :once cleanup-fixture)

(deftest pool-test
  (testing "pooled transaction"
    (let [conn-pool (pool/conn-pool redis-conn conn/commands-sync)]
      (pool/with-conn-pool conn-pool cmds
        (redis/multi cmds)
        (redis/set   cmds :a 1)
        (redis/set   cmds :b 2)
        (redis/get   cmds :a)
        (redis/get   cmds :b)
        (is (= ["OK" "OK" 1 2] (redis/exec cmds))))
      (pool/close conn-pool)))

  (testing "async pooled transaction with future"
    (let [conn-pool (pool/conn-pool redis-conn conn/commands-sync)
          res (pool/with-conn-pool* conn-pool cmds c
                (future ;; simulating async commands
                  (redis/multi cmds)
                  (redis/set   cmds :c 3)
                  (redis/set   cmds :d 4)
                  (redis/get   cmds :c)
                  (redis/get   cmds :d)
                  (let [res (redis/exec cmds)]
                    (pool/return-conn conn-pool c)
                    res)))]
      (is (= ["OK" "OK" 3 4] @res))
      (pool/close conn-pool))))
