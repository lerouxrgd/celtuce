(defproject celtuce "0.2.0-SNAPSHOT"
  :description "An idiomatic Clojure Redis client wrapping the Java client Lettuce"
  :url "https://github.com/lerouxrgd/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [biz.paluch.redis/lettuce "4.4.1-SNAPSHOT"]
                 [potemkin "0.4.4"]
                 [manifold "0.1.6"]
                 [com.taoensso/nippy "2.13.0"]
                 [com.twitter/carbonite "1.5.0"]])
