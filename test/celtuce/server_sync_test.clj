(ns celtuce.server-sync-test
  (:require 
   [clojure.test :refer :all]
   [celtuce.commands :as redis]
   [celtuce.connector :as conn]))

(def redis-url "redis://localhost:6379")
(def ^:dynamic *cmds*)

(defmacro with-str-cmds [& body]
  `(let [rserv# (conn/redis-server redis-url
                                   :codec (celtuce.codec/utf8-string-codec))]
     (binding [*cmds* (conn/commands-sync rserv#)]
       (try ~@body
            (finally (conn/shutdown rserv#))))))

(defmacro with-pubsub-cmds 
  "Binds local @pub and @sub with different connections, 
  registers the given listener on @sub"
  [listener & body]
  `(let [rserv-pub# (conn/as-pubsub (conn/redis-server redis-url))
         rserv-sub# (conn/as-pubsub (conn/redis-server redis-url))]
     (conn/add-listener! rserv-sub# ~listener)
     (with-local-vars
       [~'pub (conn/commands-sync rserv-pub#)
        ~'sub (conn/commands-sync rserv-sub#)]
       (try ~@body
            (finally (conn/shutdown rserv-pub#)
                     (conn/shutdown rserv-sub#))))))

(defn cmds-fixture [test-function]
  (let [rserv (conn/redis-server redis-url)]
    (binding [*cmds* (conn/commands-sync rserv)]
      (try (test-function)
           (finally (conn/shutdown rserv))))))

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
    (is (= ["bar" 1 nil :b nil]
           (redis/hmget *cmds* "h" [:foo :a 0 "b" :dont-exist])))
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
    (let [cur (redis/hscan
               *cmds* "hl" (redis/scan-cursor) (redis/scan-args :limit 10))
          res (redis/scan-res cur)]
      (is (= false (celtuce.scan/finished? cur)))
      (is (= true (map? res)))
      (is (<= 5 (count res) 15))) ;; about 10
    (let [els1 (->> (redis/scan-args :limit 50)
                    (redis/hscan *cmds* "hl" (redis/scan-cursor))
                    (redis/chunked-scan-seq) 
                    (take 100))
          els2 (redis/hscan-seq *cmds* "hl")]
      (is (<= 95 (count els1) 105)) ;; about 100
      (is (= (redis/hgetall *cmds* "hl")
             (apply merge els1)
             (into {} els2))))
    (redis/hmset
     *cmds* "hs" (->> (range 100) (map str) (split-at 50) (apply zipmap)))
    (let [cur (redis/hscan
               *cmds* "hs" (redis/scan-cursor) (redis/scan-args :match "*0"))
          res (redis/scan-res cur)]
      (is (= true (celtuce.scan/finished? cur)))
      (is (= (->> (range 0 50 10) (map (fn [x] [(str x) (str (+ x 50))])) (into {}))
             res)))
    (is (thrown? Exception
                 (redis/chunked-scan-seq
                  (redis/hscan *cmds* nil (redis/scan-cursor)))))))

(deftest key-commands-test
  
  (testing "basic key checks"
    (redis/hmset *cmds* "h" {:foo "bar" :a 1 "b" :b})
    (is (= 1 (redis/exists   *cmds* "h")))
    (is (= 1 (redis/mexists  *cmds* ["h" :dont-exist])))
    (is (= ["h"] (redis/keys *cmds* "h")))
    (is (= ["h"] (->> (redis/scan *cmds*)
                      (redis/chunked-scan-seq) 
                      (apply concat)
                      (into []))))
    (is (= ["h"] (redis/scan-seq *cmds*)))
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
    (let [args (redis/bitfield-args
                :incrby :u2 100 1 :overflow :sat :incrby :u2 102 1)]
      (is (= [1 1] (redis/bitfield *cmds* "bf" args)))
      (is (= [2 2] (redis/bitfield *cmds* "bf" args)))
      (is (= [3 3] (redis/bitfield *cmds* "bf" args)))
      (is (= [0 3] (redis/bitfield *cmds* "bf" args))))))

(deftest list-commands-test

  (testing "basic list manipulations"
    (is (= 0 (redis/rpushx *cmds* "x" :no-op)))
    (is (= 5 (redis/mrpush *cmds* "l" (->> (range 65 70) (map char)))))
    (is (= 5 (redis/llen   *cmds* "l")))
    (is (= [\A \B \C \D \E] (redis/lrange *cmds* "l" 0 5)))
    (is (= 6  (redis/lpush   *cmds* "l" \A)))
    (is (= 2  (redis/lrem    *cmds* "l" 2 \A)))
    (is (= \B (redis/lindex  *cmds* "l" 0)))
    (is (= 5  (redis/linsert *cmds* "l" true \B \A)))
    (redis/lset  *cmds* "l" 2 \Z)
    (redis/ltrim *cmds* "l" 0 2)
    (is (= [\A \B \Z] (redis/lrange *cmds* "l" 0 5)))
    (is (= \Z (redis/rpop *cmds* "l")))
    (is (= 2  (redis/llen *cmds* "l"))))

  (testing "list blocking commands"
    (redis/mrpush *cmds* "bl" [1 2 3])
    (is (= ["bl" 1] (redis/blpop *cmds* 1 ["bl"])))
    (is (= ["bl" 3] (redis/brpop *cmds* 1 ["bl"])))))

(deftest set-commands-test

  (testing "add and scan set members"
    (is (= 1 (redis/sadd  *cmds* "s1" :a)))
    (is (= 4 (redis/msadd *cmds* "s1" [:b :c :d :e])))
    (is (= 5 (redis/scard *cmds* "s1")))
    (is (= true (redis/sismember *cmds* "s1" :a)))
    (is (= #{:a :b :c :d :e}
           (redis/smembers *cmds* "s1")))
    (is (= #{:a :b :c :d :e}
           (->> (redis/sscan *cmds* "s1" (redis/scan-cursor))
                (redis/chunked-scan-seq) 
                (take 5)
                (apply into #{}))))
    (is (= #{:a :b :c :d :e}
           (into #{} (redis/sscan-seq *cmds* "s1")))))

  (testing "deleting set members"
    (let [m    (redis/spop     *cmds* "s1")
          ms   (redis/smembers *cmds* "s1")]
      (is (= 4 (redis/scard    *cmds* "s1")))
      (is (= 4 (count ms)))
      (is (= #{:a :b :c :d :e}
             (conj ms m))))
    (let [m        (redis/srandmember *cmds* "s1")]
      (is (= 1     (redis/srem        *cmds* "s1" m)))
      (is (= 3     (redis/scard       *cmds* "s1")))
      (is (= false (redis/sismember   *cmds* "s1" m))))))

(deftest sortedset-commands-test

  (testing "add, incr, count sorted set members"
    (is (= 1   (redis/zadd     *cmds* "z1" 0.1 :a)))
    (is (= 1   (redis/zadd     *cmds* "z1" :nx 0.2 :b)))
    (is (= 3   (redis/mzadd    *cmds* "z1" [[0.3 :c] [0.4 :d] [0.5 :e]])))
    (is (= 5   (redis/zcard    *cmds* "z1")))
    (is (= 0.6 (redis/zaddincr *cmds* "z1" 0.1 :e)))
    (is (= 0.6 (redis/zscore   *cmds* "z1" :e)))
    (is (= 0.5 (redis/zincrby  *cmds* "z1" -0.1 :e)))
    (is (= 3   (redis/zcount   *cmds* "z1" 0.3 0.5)))
    (is (= 0   (redis/zrank    *cmds* "z1" :a)))
    (is (= 4   (redis/zrevrank *cmds* "z1" :a))))

  (testing "range, revrange, scan sorted set"
    (is (= [:a :b :c]
           (redis/zrange *cmds* "z1" 0 2)))
    (is (= [[0.1 :a] [0.2 :b] [0.3 :c]]
           (redis/zrange-withscores *cmds* "z1" 0 2)))
    (is (= [:c :d :e]
           (redis/zrangebyscore *cmds* "z1" 0.3 0.5)))
    (is (= [[0.3 :c] [0.4 :d] [0.5 :e]]
           (redis/zrangebyscore-withscores *cmds* "z1" 0.3 0.5)))
    (is (= [:e :d :c]
           (redis/zrevrange *cmds* "z1" 0 2)))
    (is (= [[0.5 :e] [0.4 :d] [0.3 :c]]
           (redis/zrevrange-withscores *cmds* "z1" 0 2)))
    (is (= [:e :d :c]
           (redis/zrevrangebyscore *cmds* "z1" 0.5 0.3)))
    (is (= [[0.5 :e] [0.4 :d] [0.3 :c]]
           (redis/zrevrangebyscore-withscores *cmds* "z1" 0.5 0.3)))
    (is (= #{[0.1 :a] [0.2 :b] [0.3 :c] [0.4 :d] [0.5 :e]}
           (->> (redis/zscan *cmds* "z1" (redis/scan-cursor))
                (redis/chunked-scan-seq)
                (take 5)
                (apply into #{}))))
    (is (= #{[0.1 :a] [0.2 :b] [0.3 :c] [0.4 :d] [0.5 :e]}
           (into #{} (redis/zscan-seq *cmds* "z1")))))

  (testing "deleting sorted set members"
    (is (= 1  (redis/zrem  *cmds* "z1" :a)))
    (is (= :b (first (redis/zrange *cmds* "z1" 0 0))))
    (is (= 2  (redis/mzrem *cmds* "z1" [:b :c])))
    (is (= :d (first (redis/zrange *cmds* "z1" 0 0))))
    (is (= 1  (redis/zremrangebyrank *cmds* "z1" 0 0)))
    (is (= :e (first (redis/zrange *cmds* "z1" 0 0))))
    (is (= 1  (redis/zremrangebyscore *cmds* "z1" 0.5 0.5)))
    (is (= 0  (redis/zcard *cmds* "z1"))))

  (testing "lexicographical order based commands"
    (with-str-cmds
      (redis/mzadd *cmds* "z2" [[0.0 "a"] [0.0 "b"] [0.0 "c"] [0.0 "d"] [0.0 "e"]])
      (is (= ["a" "b" "c"] 
             (redis/zrangebylex *cmds* "z2" "-" "[c")))
      (is (= 5 (redis/zlexcount *cmds* "z2" "-" "+"))))))

(deftest scripting-commands-test
  (let [script "return 10"
        sha    "080c414e64bca1184bc4f6220a19c4d495ac896d"]
    (with-str-cmds
      (testing "simple scripting"
        (is (= 10  (redis/eval    *cmds* script :integer [])))
        (is (= sha (redis/digest  *cmds* script)))
        (is (= 10  (redis/evalsha *cmds* sha :integer [])))
        (redis/script-flush *cmds*)
        (is (thrown? com.lambdaworks.redis.RedisCommandExecutionException
                     (redis/evalsha *cmds* sha :integer [])))))))

(deftest hll-commands-test
  (let [err 0.81
        close? (fn [x y] (<= (- x (* x err)) y (+ x (* x err))))]
    (testing "basic hll usage"
      (is (= 1 (redis/pfadd  *cmds* "pf1" 0)))
      (is (= 1 (redis/mpfadd *cmds* "pf1" (range 1 1000))))
      (is (close? 1000 (redis/pfcount  *cmds* "pf1")))
      (is (= 0 (redis/mpfadd *cmds* "pf1" (range 1000))))
      (is (close? 1000 (redis/pfcount  *cmds* "pf1")))
      (is (= 1 (redis/mpfadd *cmds* "pf1" (range 1000 2000))))
      (is (close? 2000 (redis/pfcount  *cmds* "pf1"))))))

(deftest geo-commands-test
  (let [err 0.999999
        close? (fn [x y] (<= (- x (* x err)) y (+ x (* x err))))]

    (testing "basic geo usage"
      (is (= 1 (redis/geoadd *cmds* "Sicily" 13.361389 38.115556 "Palermo")))
      (is (= 2 (redis/geoadd *cmds* "Sicily" [[15.087269 37.502669 "Catania"]
                                              [13.583333 37.316667 "Agrigento"]])))
      (is (= (redis/geohash  *cmds* "Sicily" "Agrigento") "sq9sm1716e0"))
      (is (= (redis/mgeohash *cmds* "Sicily" ["Palermo" "Catania"])
             ["sqc8b49rny0" "sqdtr74hyu0"])))

    (testing "georadius, by coord and by member"
      (is (= (redis/georadius *cmds* "Sicily" 15 37 200 :km)
             #{"Agrigento" "Catania" "Palermo"}))
      (let [[palermo agrigento]
            (redis/georadius *cmds* "Sicily" 15 37 200 :km
                             (redis/geo-args :with-dist true :with-coord true
                                             :count 2 :sort :desc))]
        (is (= "Palermo"      (-> palermo :member)))
        (is (close? 190.4424  (-> palermo :distance)))
        (is (close? 13.361389 (-> palermo :coordinates :x)))
        (is (close? 38.115556 (-> palermo :coordinates :y)))
        (is (= "Agrigento"    (-> agrigento :member)))
        (is (close? 130.4235  (-> agrigento :distance)))
        (is (close? 13.583333 (-> agrigento :coordinates :x)))
        (is (close? 37.316667 (-> agrigento :coordinates :y))))
      (is (= (redis/georadiusbymember *cmds* "Sicily" "Agrigento" 100 :km)
             #{"Agrigento" "Palermo"}))
      (let [[agrigento palermo]
            (redis/georadiusbymember
             *cmds* "Sicily" "Agrigento" 100 :km
             (redis/geo-args :with-dist true :with-coord true))]
        (is (= "Agrigento"    (-> agrigento :member)))
        (is (close? 0.0000    (-> agrigento :distance)))
        (is (close? 13.583333 (-> agrigento :coordinates :x)))
        (is (close? 37.316667 (-> agrigento :coordinates :y)))
        (is (= "Palermo"      (-> palermo :member)))
        (is (close? 90.9778   (-> palermo :distance)))
        (is (close? 13.361389 (-> palermo :coordinates :x)))
        (is (close? 38.115556 (-> palermo :coordinates :y)))))

    (testing "position and distance"
      (let [palermo (redis/geopos *cmds* "Sicily" "Palermo")]
        (is (close? 13.361389 (-> palermo :x)))
        (is (close? 38.115556 (-> palermo :y))))
      (let [[catania agrigento dont-exist]
            (redis/mgeopos *cmds* "Sicily" ["Catania" "Agrigento" "DontExist"])]
        (is (close? 15.087269 (-> catania :x)))
        (is (close? 37.502669(-> catania :y)))
        (is (close? 13.583333 (-> agrigento :x)))
        (is (close? 37.316667 (-> agrigento :y)))
        (is (nil? dont-exist)))
      (is (close? 166.2742 (redis/geodist *cmds* "Sicily" "Palermo" "Catania" :km)))
      (is (close? 103.3182 (redis/geodist *cmds* "Sicily" "Palermo" "Catania" :mi))))))

(deftest transactional-commands-test
  (testing "basic transaction"
    (redis/multi *cmds*)
    (redis/set   *cmds* :a 1)
    (redis/set   *cmds* :b 2)
    (redis/get   *cmds* :a)
    (redis/get   *cmds* :b)
    (is (= ["OK" "OK" 1 2] (redis/exec *cmds*)))))

(deftest pubsub-commands-test
  (testing "simple pub/sub mechanism"
    (let [nb-sub (atom 0)
          subscribed? (promise)
          res (promise)
          unsubscribed? (promise)]
      (with-pubsub-cmds
        (reify redis/PubSubListener
          (message [_ channel message]
            (deliver res [channel message]))
          (message [_ pattern channel message])
          (subscribed [_ channel count]
            (swap! nb-sub inc)
            (deliver subscribed? true))
          (unsubscribed [_ channel count]
            (swap! nb-sub dec)
            (deliver unsubscribed? true))
          (psubscribed [_ pattern count])
          (punsubscribed [_ pattern count]))
        (redis/subscribe @sub "c")
        (is (= true @subscribed?))
        (is (= 1 @nb-sub))
        (redis/publish @pub "c" "message")
        (is (= ["c" "message"] @res))
        (redis/unsubscribe @sub "c")
        (is (= true @unsubscribed?))
        (is (= 0 @nb-sub)) ))))

