(ns clj-lettuce.scan
  (:import [com.lambdaworks.redis ScanArgs ScanCursor
            KeyScanCursor ValueScanCursor MapScanCursor ScoredValueScanCursor]))

(defn ^ScanArgs scan-args [& {limit :limit match :match}]
  (cond-> (ScanArgs.)
    (not= nil limit) (.limit (long limit))
    (not= nil match) (.match ^String match)))

(defprotocol PScanCursor
  (get-cursor [this] "Get the String cursor id")
  (finished?  [this] "True if the scan operation of this cursor is finished"))

(defprotocol PScanResult
  (scan-res [this] "Get the data contained in a scan cursor result"))

(extend-type ScanCursor
  PScanCursor
  (get-cursor [this] (.getCursor  this))
  (finished?  [this] (.isFinished this)))

(extend-protocol PScanResult
  MapScanCursor
  (scan-res [this] (into {} (.getMap this))))

(defn ^ScanCursor scan-cursor 
  ([]
   ScanCursor/INITIAL)
  ([cursor]
   (doto (ScanCursor.)
     (.setCursor cursor))))

(defn scan-seq* [scan-fn cursor args]
  (let [cursor (or cursor (scan-cursor))
        res (if args (scan-fn cursor args) (scan-fn cursor))]
    (lazy-seq (cons (scan-res res)
                    (scan-seq* scan-fn (scan-cursor (get-cursor res)) args)))))

(defmacro scan-seq 
  "Takes a scan command form (scan, sscan, hscan, zscan) 
  with optionals ScanCursor c and ScanArgs args, 
  and returns a lazy seq that calls scan-res on each iteration result."
  [scan-form]
  (let [[scan-cmd this k c args] scan-form]
    `(scan-seq* (partial ~scan-cmd ~this ~k) ~c ~args)))

