(ns clj-lettuce.args.scan
  (:import 
   (com.lambdaworks.redis 
    ScanArgs ScanCursor
    KeyScanCursor ValueScanCursor MapScanCursor ScoredValueScanCursor)))

(defn ^ScanArgs scan-args [& {limit :limit match :match}]
  (cond-> (ScanArgs.)
    limit (.limit (long limit))
    match (.match ^String match)))

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
  (let [[scan-cmd this a1 a2 a3] scan-form]
    `(cond 
       ;; scan-cmd is SCAN
       (or (every? nil? [~a1 ~a1 ~a2]) (instance? ScanCursor ~a1))
       (scan-seq* (partial ~scan-cmd ~this) ~a1 ~a2)
       ;; scan-cmd is SSCAN, HSCAN, or ZSCAN
       (not= nil ~a1)
       (scan-seq* (partial ~scan-cmd ~this ~a1) ~a2 ~a3)
       ;; invalid arguments for the given scan-cmd
       :else (ex-info (->> ["invalid arguments" '~a1 '~a2 '~a3 "for" '~scan-cmd] 
                           (interpose " ")
                           (apply str))))))

