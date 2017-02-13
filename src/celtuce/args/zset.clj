(ns celtuce.args.zset
  (:import 
   (com.lambdaworks.redis ZAddArgs ZAddArgs$Builder ZStoreArgs)))

(defn ^ZAddArgs zadd-args [opt]
  (case opt
    :nx (ZAddArgs$Builder/nx)
    :xx (ZAddArgs$Builder/xx)
    :ch (ZAddArgs$Builder/ch)
    (throw (ex-info "invalid zadd opt" {:opt opt}))))

(defn zstore-args [agg & weights]
  (cond-> (ZStoreArgs.)
    (not (empty? weights)) (.weights (into-array Long/TYPE weights)) 
    (= :sum agg) (.sum)
    (= :min agg) (.min)
    (= :max agg) (.max)))

