(ns celtuce.args.kill
  (:import 
   (io.lettuce.core KillArgs KillArgs$Builder)))

(defn ^KillArgs kill-args [& {skipme :skipme addr :addr id :id type :type}]
  {:pre [(or (nil? type)
             (#{:pubsub :normal :slave} type))]}
  (-> (if type
        (cond
          (= :pubsub type) (KillArgs$Builder/typePubsub)
          (= :normal type) (KillArgs$Builder/typeNormal)
          (= :slave  type) (KillArgs$Builder/typeSlave))
        (KillArgs.))
      (cond-> 
          skipme (.skipme skipme)
          addr   (.addr addr)
          id     (.id id))))

