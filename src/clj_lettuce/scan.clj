(ns clj-lettuce.scan
  (:import [com.lambdaworks.redis ScanArgs ScanCursor
            MapScanCursor KeyScanCursor ValueScanCursor ScoredValueScanCursor]))

(defn ^ScanArgs scan-args [& {limit :limit match :match}]
  (cond-> (ScanArgs.)
    (not= nil limit) (.limit (long limit))
    (not= nil match) (.match ^String match)))

(defprotocol PScanCursor
  (get-cursor   [this]          "Get the String cursor id")
  (set-cursor   [this cursor]   "Set the String cusor id")
  (finished?    [this]          "True if the scan operation of this cursor is finished")
  (set-finished [this finished] "Set finished value (Boolean) of this cursor"))

(defprotocol PScanResult
  (scan-res [this] "Get the data contained in a scan cursor result"))

(extend-type ScanCursor
  PScanCursor
  (get-cursor   [this]   (.getCursor   this))
  (set-cursor   [this c] (.setCursor   this c))
  (finished?    [this]   (.isFinished  this))
  (set-finished [this f] (.setFinished this f)))

(extend-protocol PScanResult
  MapScanCursor
  (scan-res [this] (into {} (.getMap this))))

(defn ^ScanCursor scan-cursor 
  ([]
   ScanCursor/INITIAL)
  ([cursor]
   (doto (ScanCursor.)
     (.setCursor cursor))))

;; TODO lazy-seq (immutable cursors?) to realize full scan
#_
(defn scan-seq)
