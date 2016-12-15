(ns clj-lettuce.args.zset
  (:import 
   (com.lambdaworks.redis ZAddArgs ZAddArgs$Builder ZStoreArgs)))

(defn zadd-args [opt]
  (case opt
    :nx (ZAddArgs$Builder/nx)
    :xx (ZAddArgs$Builder/xx)
    :ch (ZAddArgs$Builder/ch)
    (throw (ex-info "invalid opt" {:opt opt}))))

(defn zstore-args [agg & weights]
  (cond-> (ZStoreArgs.)
    (not (empty? weights)) (.weights (into-array Long/TYPE weights)) 
    (= :sum agg) (.sum)
    (= :min agg) (.min)
    (= :max agg) (.max)))

