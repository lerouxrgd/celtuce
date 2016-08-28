(ns clj-lettuce.util.codec
  (:require 
   [carbonite.api :refer [default-registry]]
   [taoensso.nippy :as nippy])
  (:import 
   (com.lambdaworks.redis.codec 
    RedisCodec Utf8StringCodec ByteArrayCodec 
    CompressionCodec CompressionCodec$CompressionType)
   (java.io ByteArrayOutputStream ByteArrayInputStream)
   (java.nio ByteBuffer)
   (com.esotericsoftware.kryo Kryo)
   (com.esotericsoftware.kryo.io Output Input)
   (com.esotericsoftware.kryo.pool KryoFactory KryoPool KryoPool$Builder)))

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

(defn kryo-read 
  "Deserialize obj from ByteBuffer bb"
  [^Kryo kryo bb]
  (with-open [in (Input. (ByteArrayInputStream. (bb->bytes bb)))]
    (.readClassAndObject kryo in)))

(defn kryo-write 
  "Serialize obj to ByteBuffer"
  [^Kryo kryo obj]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [out (Output. bos)]
      (.writeClassAndObject kryo out obj))
    (bytes->bb (.toByteArray bos))))

(defn kryos-pool
  "Kryo objects pool with soft references to allow for GC when running out of memory"
  [kryo-factory]
  (-> (proxy [KryoFactory] []
        (create []
          (kryo-factory)))
      (KryoPool$Builder.)
      .softReferences
      .build))

(defmacro with-kryos-pool 
  "Inject a Kryo object from kryo-pool as the first parameter of form and run it"
  [kryo-pool form]
  (let [kryo-pool (vary-meta kryo-pool assoc :tag `KryoPool)]
    `(let [kryo# (.borrow ~kryo-pool)
           res# (-> kryo# ~form)]
       (.release ~kryo-pool kryo#)
       res#)))

(defn carbonite-codec 
  ([]
   (carbonite-codec (fn [] (default-registry))))
  ([kryo-factory]
   (let [kryos (kryos-pool kryo-factory)]
     (proxy [RedisCodec] []
       (decodeKey [bb]
         (with-kryos-pool kryos (kryo-read bb)))
       (decodeValue [bb]
         (with-kryos-pool kryos (kryo-read bb)))
       (encodeKey [k]
         (with-kryos-pool kryos (kryo-write k)))
       (encodeValue [v]
         (with-kryos-pool kryos (kryo-write v)))))))

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

