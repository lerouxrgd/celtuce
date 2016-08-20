(ns clj-lettuce.codec
  (:import [com.lambdaworks.redis.codec 
            RedisCodec Utf8StringCodec ByteArrayCodec 
            CompressionCodec CompressionCodec$CompressionType]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.nio ByteBuffer]
           [com.esotericsoftware.kryo Kryo]
           [com.esotericsoftware.kryo.io Output Input])
  (:require [carbonite.api :refer [default-registry]]
            [taoensso.nippy :as nippy]))

(defn bb->bytes [^ByteBuffer bb]
  (let [bytes (byte-array (.remaining bb))]
    (.get bb bytes)
    bytes))

(defn bytes->bb [^bytes b]
  (ByteBuffer/wrap b))

;; Lettuce codecs

(defn utf8-string-codec []
  (Utf8StringCodec.))

(defn byte-array-codec []
  (ByteArrayCodec/INSTANCE))

(defn compression-codec [^RedisCodec delegate-codec compression-type]
  {:pre [(#{:gzip :deflate} compression-type)]}
  (let [type (case compression-type
               :gzip    CompressionCodec$CompressionType/GZIP
               :deflate CompressionCodec$CompressionType/DEFLATE)]
    (CompressionCodec/valueCompressor delegate-codec type)))

;; Carbonite based codec

(defn kryo-read [^Kryo registry ^ByteBuffer bb]
  (with-open [in (Input. (ByteArrayInputStream. (bb->bytes bb)))]
    (.readClassAndObject registry in)))

(defn kryo-write [^Kryo registry obj]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [out (Output. bos)]
      (.writeClassAndObject registry out obj))
    (bytes->bb (.toByteArray bos))))

(defn thread-local-kryos [kryo-factory]
  (proxy [ThreadLocal] []
    (initialValue []
      (kryo-factory))))

(defn carbonite-codec 
  ([]
   (carbonite-codec (constantly (default-registry))))
  ([kryo-factory]
   (let [^ThreadLocal kryos (thread-local-kryos kryo-factory)]
     (proxy [RedisCodec] []
       (decodeKey [bb]         
         (kryo-read  (.get kryos) bb))
       (decodeValue [bb]
         (kryo-read  (.get kryos) bb))
       (encodeKey [k]
         (kryo-write (.get kryos) k))
       (encodeValue [v]
         (kryo-write (.get kryos) v))))))

;; Nippy based codec

(defn nippy-codec
  ([]
   (nippy-codec nil nil))
  ([freeze-opts thaw-opts]
   (proxy [RedisCodec] []
     (decodeKey [bb]
       (nippy/thaw (bb->bytes bb) thaw-opts))
     (decodeValue [bb]
       (nippy/thaw (bb->bytes bb) thaw-opts))
     (encodeKey [k]
       (bytes->bb (nippy/freeze k freeze-opts)))
     (encodeValue [v]
       (bytes->bb (nippy/freeze v freeze-opts))))))

