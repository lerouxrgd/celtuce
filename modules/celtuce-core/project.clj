(defproject celtuce-core celtuce-version
  :description "An idiomatic Clojure Redis client wrapping the Java client Lettuce"
  :url "https://github.com/lerouxrgd/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [biz.paluch.redis/lettuce "4.4.1.Final"]
                 [potemkin "0.4.4"]
                 [com.taoensso/nippy "2.13.0"]
                 [com.twitter/carbonite "1.5.0"]]
  :global-vars {*warn-on-reflection* true})
