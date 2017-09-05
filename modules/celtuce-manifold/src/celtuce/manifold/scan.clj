(ns celtuce.manifold.scan
  (:require
   [celtuce.scan :refer [scan-res PScanResult]]
   [manifold.deferred])
  (:import
   (manifold.deferred Deferred)))

(extend-protocol PScanResult
  Deferred
  (scan-res [this]
    (scan-res @this)))

