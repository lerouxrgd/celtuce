(ns celtuce.cluster-dynamic-test
  (:require
   [clojure.test :refer :all]
   [celtuce.connector :as conn]))

(def redis-url "redis://localhost:30001")
(def ^:dynamic *cmds*)

(gen-interface
  :name io.celtuce.MyCommands
  :extends [io.lettuce.core.dynamic.Commands]
  :methods
  [[^{io.lettuce.core.dynamic.annotation.Command "GET"}
    myGetRaw
    [String]
    bytes]
   [^{io.lettuce.core.dynamic.annotation.Command "GET"}
    myGet
    [String]
    String]
   [^{io.lettuce.core.dynamic.annotation.Command "SET ?0 ?1"}
    mySet
    [String String]
    String]
   [flushall
    []
    Object]
   ])

(defprotocol MyCommands
  (my-get-raw [this k])
  (my-get     [this k])
  (my-set     [this k v])
  (flushall   [this]))

(extend-type io.celtuce.MyCommands
  MyCommands
  (my-get-raw [this k]
    (.myGetRaw this k))
  (my-get [this k]
    (.myGet this k))
  (my-set [this k v]
    (.mySet this k v))
  (flushall [this]
    (.flushall this)))

(defn cmds-fixture [test-function]
  (let [rclust (conn/redis-cluster redis-url)]
    (binding [*cmds* (conn/commands-dynamic rclust io.celtuce.MyCommands)]
      (try (test-function)
           (finally (conn/shutdown rclust))))))

(defn flush-fixture [test-function]
  (flushall *cmds*)
  (test-function))

(use-fixtures :once cmds-fixture)
(use-fixtures :each flush-fixture)

(deftest my-commands-test
  (testing "set and get various keys/values"
    (my-set *cmds* "foo" "bar")
    (is (= "bar" (my-get *cmds* "foo")))
    (is (= "bar" (String. ^bytes (my-get-raw *cmds* "foo"))))
    ))
