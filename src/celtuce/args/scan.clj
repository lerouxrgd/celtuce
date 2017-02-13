(ns celtuce.args.scan
  (:import 
   (com.lambdaworks.redis 
    ScanArgs ScanCursor
    KeyScanCursor ValueScanCursor MapScanCursor ScoredValueScanCursor
    ScoredValue)))

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
  (scan-res [this] 
    (into {} (.getMap this)))
  KeyScanCursor
  (scan-res [this] 
    (into [] (.getKeys this)))
  ValueScanCursor
  (scan-res [this] 
    (into [] (.getValues this)))
  ScoredValueScanCursor
  (scan-res [this] 
    (->> (.getValues this)
         (map (fn [^ScoredValue sv] [(.score sv) (.value sv)]))
         (into []))))

(defn ^ScanCursor scan-cursor 
  ([]
   ScanCursor/INITIAL)
  ([cursor]
   (doto (ScanCursor.)
     (.setCursor cursor))))

(defn scan-seq* [scan-fn cursor args]
  (when-not (finished? cursor)
    (let [cursor-res (if args (scan-fn cursor args) (scan-fn cursor))]
      (lazy-seq (cons (scan-res cursor-res)
                      (scan-seq* scan-fn cursor-res args))))))

(defmacro scan-seq 
  "Takes a scan EXPR composed of:
   - a command: \"scan\", \"sscan\", \"hscan\", \"zscan\"
   - a key when the command is not \"scan\"
   - optionals: ScanCursor c, ScanArgs args
  Returns a lazy seq that calls scan-res on each iteration result."
  [scan-expr]
  (let [[scan-cmd this a1 a2 a3] scan-expr]
    `(cond 
       ;; scan-cmd is: SCAN
       ;; a1 is ScanCursor, a2 is ScanArgs
       (or (every? nil? [~a1 ~a2 ~a3]) (instance? ScanCursor ~a1))
       (scan-seq* (partial ~scan-cmd ~this) (or ~a1 (scan-cursor)) ~a2)
       ;; scan-cmd is: SSCAN, HSCAN, or ZSCAN
       ;; a1 is a key, a2 is ScanCursor, a3 is ScanArgs
       (not= nil ~a1)
       (scan-seq* (partial ~scan-cmd ~this ~a1) (or ~a2 (scan-cursor)) ~a3)
       ;; invalid arguments for the given scan-cmd
       :else (throw (ex-info "malformed scan command" {:scan-cmd (name '~scan-cmd)
                                                       :args ['~a1 '~a2 '~a3]})))))

