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
  (testing "set and get a values with different type"
    (redis/set *cmds* "foo"     "bar")
    (redis/set *cmds* "foofoo"  "barbar")
    #_(redis/set *cmds* "foo-int" 1) ;; TODO investigate Encoder to make this work
    #_(redis/set *cmds* "foo-kw"  :bar)
    (is (= "bar" (redis/get *cmds* "foo")))
    #_(is (= 1     (redis/get *cmds* "foo-int")))
    #_(println (redis/get *cmds* "foo-kw"))
    )
  (testing "multiget and result type (with underlying (into (empty keys) ...)"
    (is (=  ["bar" "barbar"] (redis/mget *cmds*  ["foo" "foofoo"])))
    (is (= '("barbar" "bar") (redis/mget *cmds* '("foo" "foofoo"))))))
