(ns celtuce.impl.pubsub
  (:refer-clojure :exclude [get set keys sort type eval time])
  (:require 
   [celtuce.commands :refer :all])
  (:import 
   (com.lambdaworks.redis.pubsub.api.sync RedisPubSubCommands)))

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

