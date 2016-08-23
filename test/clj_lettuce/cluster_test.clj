(ns clj-lettuce.cluster-test
  (:require [clojure.test :refer :all]
            [clj-lettuce.commands :as redis]
            [clj-lettuce.connector :as conn]))

(def ^:dynamic *cmds*)

(defn cmds-fixture [test-function]
  (let [rclust (conn/redis-cluster "redis://localhost:30001")]
    (binding [*cmds* (conn/commands-sync rclust)]
      (redis/flushall *cmds*)
      (try (test-function)
           (finally (conn/shutdown rclust))))))

(use-fixtures :once cmds-fixture)

(deftest hash-commands-test
  
  (testing "set and get multiple hash values"
    (redis/hmset *cmds* "h" {:foo "bar" :a 1 0 nil})
    (redis/hset  *cmds* "h" "b" :b)
    (is (= true  (redis/hexists *cmds* "h" :a)))
    (is (= false (redis/hexists *cmds* "h" :dont-exist)))
    (is (= nil   (redis/hget    *cmds* "h" :dont-exist)))
    (is (= false (redis/hsetnx  *cmds* "h" :a :a)))
    (is (= "bar" (redis/hget    *cmds* "h" :foo)))
    (is (= ["bar" 1 nil :b nil] (redis/hmget *cmds* "h" [:foo :a 0 "b" :dont-exist])))
    (is (= {:foo "bar" :a 1 0 nil "b" :b} (redis/hgetall *cmds* "h")))
    (is (= #{:foo :a 0 "b"} 
           (into #{} (redis/hkeys *cmds* "h"))))
    (is (= #{"bar" 1 nil :b} 
           (into #{} (redis/hvals *cmds* "h")))))

  (testing "hscan cursors"
    (redis/hmset *cmds* "hl" (->> (range 10000) (split-at 5000) (apply zipmap)))
    (let [cursor (redis/hscan *cmds* "hl" (redis/scan-cursor) (redis/scan-args :limit 10))
          result (redis/scan-res cursor)]
      (is (= false (clj-lettuce.scan/finished? cursor)))
      (is (= true (map? result)))
      (is (= 10 (count result))))
    (redis/hmset *cmds* "hs" (->> (range 100) (map str) (split-at 50) (apply zipmap)))
    (let [cursor (redis/hscan *cmds* "hs" (redis/scan-cursor) (redis/scan-args :match "*0"))
          result (redis/scan-res cursor)]
      (is (= true (clj-lettuce.scan/finished? cursor)))
      (is (= (->> (range 0 50 10) (map (fn [x] [(str x) (str (+ x 50))])) (into {}))
             result))))

  (testing "delete and multi delete hash values"
    (redis/hmdel *cmds* "h" [:a :b "b"])
    (redis/hdel  *cmds* "h" 0)
    (is (= false (redis/hexists *cmds* "h" 0)))
    (is (= {:foo "bar"} (redis/hgetall *cmds* "h"))))

  (testing "increments and length"
    (is (= 1   (redis/hlen         *cmds* "h")))
    (is (= 9   (redis/hstrlen      *cmds* "h" :foo)))
    (is (= 1   (redis/hincrby      *cmds* "h" :x 1)))
    (is (= 10  (redis/hincrby      *cmds* "h" :x 9)))
    (is (= 1.0 (redis/hincrbyfloat *cmds* "h" :y 1)))
    (is (= 3.0 (redis/hincrbyfloat *cmds* "h" :y 2.0)))))

(deftest string-commands-test

  (testing "set and get string keys/values"
    (redis/set *cmds* "foo"    "bar")
    (redis/set *cmds* "foofoo" "barbar")
    (is (= "bar"    (redis/get *cmds* "foo")))
    (is (= "barbar" (redis/get *cmds* "foofoo"))))
  
  (testing "multiget and result type (with underlying (into (empty keys) ...)"
    (is (=  ["bar" "barbar"] (redis/mget *cmds*  ["foo" "foofoo"])))
    (is (= '("barbar" "bar") (redis/mget *cmds* '("foo" "foofoo"))))
    (is (=  [nil nil]        (redis/mget *cmds*  ["dont" "exist"]))))
  
  (testing "set and get various keys/values"
    (redis/set *cmds* "foo-int" 1337)
    (redis/set *cmds* 666       "devil")
    (redis/set *cmds* :foo-kw   :bar-kw)
    (redis/set *cmds* #{1 2 3}  '(1 2 3))
    (redis/set *cmds* {:a "a"}  [:b "b"])
    (is (= 1337     (redis/get *cmds* "foo-int")))
    (is (= "devil"  (redis/get *cmds* 666)))
    (is (= :bar-kw  (redis/get *cmds* :foo-kw)))
    (is (= '(1 2 3) (redis/get *cmds* #{1 2 3})))
    (is (= [:b "b"] (redis/get *cmds* {:a "a"}))))
  
  )
