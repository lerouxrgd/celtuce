(ns clj-lettuce.cluster-test
  (:require [clojure.test :refer :all]
            [clj-lettuce.commands :as redis]
            [clj-lettuce.cluster :refer [redis-cluster]]))

(def ^:dynamic *cmds*)

(defn cmds-fixture [test-function]
  (let [rclust (redis-cluster "redis://localhost:30001")]
    (binding [*cmds* (redis/commands :cluster-sync rclust)]
      (redis/flushall *cmds*)
      (try (test-function)
           (finally (redis/close-conn rclust)
                    (redis/shutdown   rclust))))))

(use-fixtures :once cmds-fixture)

(deftest string-commands-test

  (testing "set and get string keys/values"
    (redis/set *cmds* "foo"     "bar")
    (redis/set *cmds* "foofoo"  "barbar")
    (is (= "bar"    (redis/get *cmds* "foo")))
    (is (= "barbar" (redis/get *cmds* "foofoo"))))
  
  (testing "multiget and result type (with underlying (into (empty keys) ...)"
    (is (=  ["bar" "barbar"] (redis/mget *cmds*  ["foo" "foofoo"])))
    (is (= '("barbar" "bar") (redis/mget *cmds* '("foo" "foofoo")))))
  
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
