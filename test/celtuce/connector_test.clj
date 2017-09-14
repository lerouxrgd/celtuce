(ns celtuce.connector-test
  (:require
   [clojure.test :refer :all]
   [celtuce.connector :as conn]))

(def redis-server-url  "redis://localhost:6379")
(def redis-cluster-url "redis://localhost:30001")

(deftest redis-connector-options-test

  (testing "default options for redis-server"
    (conn/redis-server
     redis-server-url
     :conn-options
     {:auto-flush true}
     :client-options
     {:ping-before-activate-connection       false
      :suspend-reconnect-on-protocol-failure false
      :cancel-commands-on-reconnect-failure  false
      :auto-reconnect                        true
      :request-queue-size                    Integer/MAX_VALUE
      :disconnected-behavior                 :default
      :socket-options
      {:timeout 10, :unit :seconds, :keep-alive false, :tcp-no-delay false}
      :ssl-options
      {:provider :jdk}}))

  (testing "default options for redis-cluster"
    (conn/redis-cluster
     redis-cluster-url
     :conn-options
     {:auto-flush true}
     :client-options
     {;; regular client-options
      :ping-before-activate-connection       false
      :suspend-reconnect-on-protocol-failure false
      :cancel-commands-on-reconnect-failure  false
      :auto-reconnect                        true
      :request-queue-size                    Integer/MAX_VALUE
      :disconnected-behavior                 :default
      :socket-options
      {:timeout 10, :unit :seconds, :keep-alive false, :tcp-no-delay false}
      :ssl-options
      {:provider :jdk}
      ;; specific cluster options
      :validate-cluster-node-membership       true
      :max-redirects                          5
      :topology-refresh-options
      {:enable-periodic-refresh               false
       :refresh-period                        {:period 60 :unit :seconds}
       :close-stale-connections               true
       :dynamic-refresh-sources               true
       :enable-adaptive-refresh-trigger       #{}
       :adaptive-refresh-triggers-timeout     {:timeout 30 :unit :seconds}
       :refresh-triggers-reconnect-attempts   5}})))

