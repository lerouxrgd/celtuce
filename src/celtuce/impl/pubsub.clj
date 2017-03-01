(ns celtuce.impl.pubsub
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require 
   [celtuce.commands :refer :all]
   [manifold.deferred :as d])
  (:import 
   (com.lambdaworks.redis.pubsub.api.sync RedisPubSubCommands)
   (com.lambdaworks.redis.pubsub.api.async RedisPubSubAsyncCommands)))

;;
;; Sync
;;

(extend-type RedisPubSubCommands
  PubSubCommands
  (publish [this channel message]
    (.publish this channel message))
  (subscribe [this channel]
    (.subscribe this (into-array Object [channel])))
  (unsubscribe [this channel]
    (.unsubscribe this (into-array Object [channel])))
  (msubscribe [this channels]
    (.subscribe this (into-array Object channels)))
  (munsubscribe [this channels]
    (.unsubscribe this (into-array Object channels)))
  (psubscribe [this pattern]
    (.psubscribe this (into-array Object [pattern])))
  (punsubscribe [this pattern]
    (.punsubscribe this (into-array Object [pattern])))
  (mpsubscribe [this patterns]
    (.psubscribe this (into-array Object patterns)))
  (mpunsubscribe [this patterns]
    (.punsubscribe this (into-array Object patterns)))
  (pubsub-channels
    ([this]
     (into [] (.pubsubChannels this)))
    ([this channel]
     (into [] (.pubsubChannels this channel))))
  (pubsub-numsub [this channel]
    (into {} (.pubsubNumsub this ^objects (into-array Object [channel]))))
  (pubsub-numpat [this]
    (.pubsubNumpat this)))

(extend-type RedisPubSubAsyncCommands
  PubSubCommands
  (publish [this channel message]
    (d/->deferred (.publish this channel message)))
  (subscribe [this channel]
    (d/->deferred (.subscribe this (into-array Object [channel]))))
  (unsubscribe [this channel]
    (d/->deferred (.unsubscribe this (into-array Object [channel]))))
  (msubscribe [this channels]
    (d/->deferred (.subscribe this (into-array Object channels))))
  (munsubscribe [this channels]
    (d/->deferred (.unsubscribe this (into-array Object channels))))
  (psubscribe [this pattern]
    (d/->deferred (.psubscribe this (into-array Object [pattern]))))
  (punsubscribe [this pattern]
    (d/->deferred (.punsubscribe this (into-array Object [pattern]))))
  (mpsubscribe [this patterns]
    (d/->deferred (.psubscribe this (into-array Object patterns))))
  (mpunsubscribe [this patterns]
    (d/->deferred (.punsubscribe this (into-array Object patterns))))
  (pubsub-channels
    ([this]
     (d/chain (d/->deferred (.pubsubChannels this))
              #(into [] %)))
    ([this channel]
     (d/chain (d/->deferred (.pubsubChannels this channel))
              #(into [] %))))
  (pubsub-numsub [this channel]
    (d/chain (d/->deferred (.pubsubNumsub this ^objects (into-array Object [channel])))
             #(into {} %)))
  (pubsub-numpat [this]
    (d/->deferred (.pubsubNumpat this))))

