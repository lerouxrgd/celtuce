(ns celtuce.args.migrate
  (:import 
   (io.lettuce.core MigrateArgs)))

(defn migrate-args [& {copy :copy replace :replace k :key ks :keys}]
  {:pre [(or (and (not= nil k)  (nil? ks))
             (and (not= nil ks) (nil? k)))]}
  (cond-> (MigrateArgs.)
    copy    (.copy)
    replace (.replace)
    k       (.key k)
    ks      (.keys ^objects (into-array Object ks))))
