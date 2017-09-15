(def celtuce-version "0.2.1")
(def clj-version "1.8.0")
(defproject celtuce celtuce-version
  :description "An idiomatic Clojure Redis client wrapping the Java client Lettuce"
  :url "https://github.com/lerouxrgd/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [celtuce-core ~celtuce-version]
                 [celtuce-pool ~celtuce-version]
                 [celtuce-manifold ~celtuce-version]]
  :profiles {:dev
             {:plugins [[lein-modules "0.3.11"]]}}
  :modules {:dirs ["modules/celtuce-core"
                   "modules/celtuce-pool"
                   "modules/celtuce-manifold"
                   "."]
            :subprocess nil}
  :global-vars {*warn-on-reflection* true})
