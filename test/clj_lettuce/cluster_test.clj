(ns clj-lettuce.cluster-test
  (:require 
   [clojure.test :refer :all]
   [clj-lettuce.commands :as redis]
   [clj-lettuce.connector :as conn]))

(def ^:dynamic *cmds*)

(defn cmds-fixture [test-function]
  (let [rclust (conn/redis-cluster "redis://localhost:30001")]
    (binding [*cmds* (conn/commands-sync rclust)]
      (try (test-function)
           (finally (conn/shutdown rclust))))))

(defmacro with-str-cmds [& body]
  `(let [rclust# (conn/redis-cluster "redis://localhost:30001"
                                     :codec (clj-lettuce.codec/utf8-string-codec))]
     (binding [*cmds* (conn/commands-sync rclust#)]
       (try ~@body
            (finally (conn/shutdown rclust#))))))

(defn flush-fixture [test-function]
  (redis/flushall *cmds*)
  (test-function))

(use-fixtures :once cmds-fixture)
(use-fixtures :each flush-fixture)

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
    (is (= 3.0 (redis/hincrbyfloat *cmds* "h" :y 2.0))))
  
  (testing "hscan cursors"
    (redis/hmset *cmds* "hl" (->> (range 10000) (split-at 5000) (apply zipmap)))
    (let [cur (redis/hscan *cmds* "hl" (redis/scan-cursor) (redis/scan-args :limit 10))
          res (redis/scan-res cur)]
      (is (= false (clj-lettuce.args.scan/finished? cur)))
      (is (= true (map? res)))
      (is (<= 8 (count res) 12))) ;; about 10
    (let [els (->> (redis/scan-args :limit 50)
                   (redis/hscan *cmds* "hl" (redis/scan-cursor))
                   (redis/scan-seq) 
                   (take 100))]
      (is (<= 95 (count els) 105)) ;; about 100
      (is (= (redis/hgetall *cmds* "hl")
             (apply merge els))))
    (redis/hmset *cmds* "hs" (->> (range 100) (map str) (split-at 50) (apply zipmap)))
    (let [cur (redis/hscan *cmds* "hs" (redis/scan-cursor) (redis/scan-args :match "*0"))
          res (redis/scan-res cur)]
      (is (= true (clj-lettuce.args.scan/finished? cur)))
      (is (= (->> (range 0 50 10) (map (fn [x] [(str x) (str (+ x 50))])) (into {}))
             res)))))

(deftest key-commands-test
  
  (testing "basic key checks"
    (redis/hmset *cmds* "h" {:foo "bar" :a 1 "b" :b})
    (is (= 1 (redis/exists   *cmds* "h")))
    (is (= 1 (redis/mexists  *cmds* ["h" :dont-exist])))
    (is (= ["h"] (redis/keys *cmds* "h")))
    (is (= ["h"] (->> (redis/scan-seq (redis/scan *cmds*)) 
                      (apply concat)
                      (into []))))
    (is (= "hash" (redis/type *cmds* "h"))))
  
  (testing "ttl related"
    (is (= -1 (redis/ttl *cmds* "h")))
    (redis/expire *cmds* "h" 1)
    (is (> 1001 (redis/pttl *cmds* "h") 0))
    (redis/persist *cmds* "h")
    (is (= -1 (redis/pttl *cmds* "h"))))
  
  (testing "dump/restore and delete"
    (let [dump (redis/dump *cmds* "h")]
      (redis/del *cmds* "h")
      (is (= 0 (redis/exists *cmds* "h")))
      (redis/restore *cmds* "h" 0 dump)
      (is (= 1 (redis/exists  *cmds* "h")))
      (is (=   (redis/hgetall *cmds* "h")
               {:foo "bar" :a 1 "b" :b})))))

(deftest string-commands-test

  (testing "set and get various keys/values"
    (redis/set *cmds* "foo-int" 1337)
    (redis/set *cmds* 666       "devil")
    (redis/set *cmds* :foo-kw   :bar-kw)
    (redis/set *cmds* #{1 2 3}  '(1 2 3))
    (redis/set *cmds* {:a "a"}  [:b "b"] (redis/set-args :ex 1))
    (is (= 1337     (redis/get  *cmds* "foo-int")))
    (is (= :bar-kw  (redis/get  *cmds* :foo-kw)))
    (is (= '(1 2 3) (redis/get  *cmds* #{1 2 3})))
    (is (= [:b "b"] (redis/get  *cmds* {:a "a"})))
    (is (> 1001     (redis/pttl *cmds* {:a "a"}) 0))
    (is (= "devil"    (redis/getset *cmds* 666 0xdeadbeef)))
    (is (= 0xdeadbeef (redis/get    *cmds* 666))))

  (testing "multiget/set and result type (with underlying (into (empty keys) ...)"
    (redis/mset *cmds* {"foo" "bar" "foofoo" "barbar"})
    (is (=  ["bar" "barbar"] (redis/mget *cmds*  ["foo" "foofoo"])))
    (is (= '("barbar" "bar") (redis/mget *cmds* '("foo" "foofoo"))))
    (is (=  [nil nil]        (redis/mget *cmds*  ["dont" "exist"]))))

  (testing "raw string manipulations"
    (with-str-cmds
      (is (= 5  (redis/append *cmds* "msg" "Hello")))
      (is (= 11 (redis/append *cmds* "msg" " World")))
      (is (= "Hello World" (redis/get *cmds* "msg")))
      (redis/setrange *cmds* "msg" 6 "Redis")
      (is (= "Hello Redis" (redis/get *cmds* "msg")))
      (redis/append *cmds* "ts" "0043")
      (redis/append *cmds* "ts" "0035")
      (is (= "0043" (redis/getrange *cmds* "ts" 0 3)))
      (is (= "0035" (redis/getrange *cmds* "ts" 4 7)))
      (redis/set *cmds* "k" "foobar")
      (is (= 6  (redis/strlen   *cmds* "k")))
      (is (= 26 (redis/bitcount *cmds* "k")))
      (is (= 4  (redis/bitcount *cmds* "k" 0 0)))
      (is (= 6  (redis/bitcount *cmds* "k" 1 1)))
      (redis/set *cmds* "i" "10")
      (is (= 11    (redis/incr        *cmds* "i")))      
      (is (= 15    (redis/incrby      *cmds* "i" 4)))      
      (is (= 11    (redis/decrby      *cmds* "i" 4)))
      (is (= 10    (redis/decr        *cmds* "i")))
      (is (= 11.11 (redis/incrbyfloat *cmds* "i" 1.11)))
      (is (= 0 (redis/setbit *cmds* "b" 0 0)))
      (is (= 0 (redis/setbit *cmds* "b" 1 1)))
      (is (= 1 (redis/setbit *cmds* "b" 1 1)))
      (is (= 1 (redis/getbit *cmds* "b" 1)))
      (is (= 1 (redis/bitpos *cmds* "b" true)))
      (redis/setbit *cmds* "b" 0 1)
      (redis/setbit *cmds* "b" 1 1)
      (redis/setbit *cmds* "b" 2 0)
      (redis/setbit *cmds* "b" 3 0)
      (redis/setbit *cmds* "b" 4 0)
      (redis/setbit *cmds* "b" 5 1)
      (is (= 2 (redis/bitpos *cmds* "b" false 0 0)))))

  (testing "bitfield command"
    (let [args (redis/bitfield-args :incrby :u2 100 1 :overflow :sat :incrby :u2 102 1)]
      (is (= [1 1] (redis/bitfield *cmds* "bf" args)))
      (is (= [2 2] (redis/bitfield *cmds* "bf" args)))
      (is (= [3 3] (redis/bitfield *cmds* "bf" args)))
      (is (= [0 3] (redis/bitfield *cmds* "bf" args))))))
