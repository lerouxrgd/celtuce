(defproject celtuce-manifold celtuce-version
  :url "https://github.com/lerouxrgd/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [celtuce ~celtuce-version]
                 [manifold "0.1.6"]]
  :global-vars {*warn-on-reflection* true})
