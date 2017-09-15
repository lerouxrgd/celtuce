(defproject celtuce-pool celtuce-version
  :url "https://github.com/lerouxrgd/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [celtuce-core ~celtuce-version]
                 [org.apache.commons/commons-pool2 "2.4.2"]]
  :global-vars {*warn-on-reflection* true})
