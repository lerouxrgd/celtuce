(defproject clj-lettuce "0.1.0-SNAPSHOT"
  :description "Clojure wrapper around the Java Redis client Lettuce"
  :url "https://github.com/lerouxrgd/clj-lettuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [biz.paluch.redis/lettuce "4.2.2.Final"]
                 [com.twitter/carbonite "1.5.0"]
                 [com.taoensso/nippy "2.12.1"]])
