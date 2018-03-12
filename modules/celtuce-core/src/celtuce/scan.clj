(ns celtuce.scan
  (:import 
   (io.lettuce.core 
    ScanArgs ScanCursor ScanIterator
    KeyScanCursor ValueScanCursor MapScanCursor ScoredValueScanCursor
    KeyValue ScoredValue)))

(defn ^ScanArgs scan-args [& {limit :limit match :match}]
  (cond-> (ScanArgs.)
    limit (.limit (long limit))
    match (.match ^String match)))

(defprotocol PScanCursor
  (get-cursor [this] "Get the String cursor id")
  (finished?  [this] "True if the scan operation of this cursor is finished"))

(extend-type ScanCursor
  PScanCursor
  (get-cursor [this] (.getCursor  this))
  (finished?  [this] (.isFinished this)))

(defprotocol PScanResult
  (scan-res [this] "Get the data contained in a scan cursor result"))

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
         (map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]))
         (into []))))

(defn ^ScanCursor scan-cursor 
  ([]
   ScanCursor/INITIAL)
  ([cursor]
   (doto (ScanCursor.)
     (.setCursor cursor))))

(defn chunked-scan-seq* [scan-fn cursor args]
  (let [cursor (if (instance? clojure.lang.IDeref cursor) @cursor cursor)]
    (when-not (finished? cursor)
      (let [cursor-res (if args (scan-fn cursor args) (scan-fn cursor))]
        (lazy-seq (cons (scan-res cursor-res)
                        (chunked-scan-seq* scan-fn cursor-res args)))))))

(defmacro chunked-scan-seq
  "Takes a scan EXPR composed of:
   - a command: \"scan\", \"sscan\", \"hscan\", \"zscan\"
   - a key when the command is not \"scan\"
   - optionals: ScanCursor c, ScanArgs args
  Returns a lazy seq that calls scan-res on each cursor iteration result (chunk)."
  [scan-expr]
  (let [[scan-cmd this a1 a2 a3] scan-expr]
    `(cond 
       ;; scan-cmd is: SCAN
       ;; a1 is ScanCursor, a2 is ScanArgs
       (or (every? nil? [~a1 ~a2 ~a3]) (instance? ScanCursor ~a1))
       (chunked-scan-seq* (partial ~scan-cmd ~this) (or ~a1 (scan-cursor)) ~a2)
       ;; scan-cmd is: SSCAN, HSCAN, or ZSCAN
       ;; a1 is a key, a2 is ScanCursor, a3 is ScanArgs
       (not= nil ~a1)
       (chunked-scan-seq* (partial ~scan-cmd ~this ~a1) (or ~a2 (scan-cursor)) ~a3)
       ;; invalid arguments for the given scan-cmd
       :else (throw (ex-info "malformed scan command" {:scan-cmd (name '~scan-cmd)
                                                       :args ['~a1 '~a2 '~a3]})))))

(defn scan-seq
  "Lazy SCAN sequence, takes optional scan-args"
  ([cmds]
   (iterator-seq (ScanIterator/scan cmds)))
  ([cmds args]
   (iterator-seq (ScanIterator/scan cmds ^ScanArgs args))))

(defn hscan-seq
  "Lazy HSCAN sequence, takes optional scan-args"
  ([cmds key]
   (->> (ScanIterator/hscan cmds key)
        (iterator-seq)
        (map (fn [^KeyValue kv] [(.getKey kv) (.getValue kv)]))))
  ([cmds key args]
   (->> (ScanIterator/hscan cmds key ^ScanArgs args)
        (iterator-seq)
        (map (fn [^KeyValue kv] [(.getKey kv) (.getValue kv)])))))

(defn sscan-seq
  "Lazy SSCAN sequence, takes optional scan-args"
  ([cmds key]
   (iterator-seq (ScanIterator/sscan cmds key)))
  ([cmds key args]
   (iterator-seq (ScanIterator/sscan cmds key ^ScanArgs args))))

(defn zscan-seq
  "Lazy ZSCAN sequence, takes optional scan-args"
  ([cmds key]
   (->> (ScanIterator/zscan cmds key)
        (iterator-seq)
        (map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)]))))
  ([cmds key args]
   (->> (ScanIterator/zscan cmds key)
        (iterator-seq)
        (map (fn [^ScoredValue sv] [(.getScore sv) (.getValue sv)])))))

