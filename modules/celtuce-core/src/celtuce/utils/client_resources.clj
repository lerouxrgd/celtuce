(ns celtuce.utils.client-resources
  "A namespace to host the wrapper for ClientResource and friends; this is especially useful when tuning the client side of redis connections, like
  - Netty resources
  - metrics and tracing (open tracing)"
  (:import (io.lettuce.core.resource ClientResources ClientResources$Builder)
           (java.util.concurrent TimeUnit)))


(defn create-client-resource
  "You can create an instance of client resources in a clojuresque way; check out the class io.lettuce.core.resource.ClientResources for details.
  It is useful to configure \"plumbing\" of client side redis connections such as: Netty threads, metrics, etc.
  But also it is good to have it for sharing the same NIO layer across multiple connections.
  Currently only the number of threads are implemented.
  Also, you can call it without any param or with an empty map and it will create a default client resource, but that can be shared across client connections."
  [options-map]
  (let [builder (ClientResources/builder)]
    (cond-> builder
      (contains? options-map :nb-io-threads)
      (.ioThreadPoolSize (:nb-io-threads options-map))
      (contains? options-map :nb-worker-threads)
      (.computationThreadPoolSize (:nb-worker-threads options-map)))
    (.build builder)))


(defn destroy-client-resource
  "If you create a client resource, you must close/dispose it; otherwise you will not shutdown the Netty threads."
  [^ClientResources client-resources]
  (.shutdown client-resources 100 1000 TimeUnit/MILLISECONDS))




