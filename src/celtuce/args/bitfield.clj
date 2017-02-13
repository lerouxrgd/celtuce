(ns celtuce.args.bitfield
  (:import 
   (com.lambdaworks.redis BitFieldArgs BitFieldArgs$OverflowType)))

(defn bft 
  "Constructs a BitFieldType from a keyword"
  [bft-kw]
  (if-let [[_ sign bits] (re-find #"(^[us])(\d+)$" ((fnil name "") bft-kw))]
    (case sign
      "s" (BitFieldArgs/signed   (Integer/parseInt bits))
      "u" (BitFieldArgs/unsigned (Integer/parseInt bits)))
    (throw 
     (ex-info "invalid bitfield type keyword"
              {:value bft-kw :valid #"(^[us])(\d+)$"}))))

(defn bitfield-args 
  "Constructs a BitFieldArgs from a chain of commands"
  [& commands]
  (loop [args (BitFieldArgs.)
         [sub & tail] commands]
    (case sub
      :overflow
      (let [[behavior & tail] tail]
        (case behavior
          :wrap (.overflow args BitFieldArgs$OverflowType/WRAP)
          :sat  (.overflow args BitFieldArgs$OverflowType/SAT)
          :fail (.overflow args BitFieldArgs$OverflowType/FAIL)
          (throw 
           (ex-info "invalid :overflow"
                    {:value behavior :valid #{:wrap :sat :fail}})))
        (if (nil? tail)
          (throw
           (ex-info (str "no sub-command after :overflow " behavior)
                    {:value tail :valid #{:get :set :incrby}}))
          (recur args tail)))
      :get
      (let [[bft-kw offset & tail] tail]
        (.get args (bft bft-kw) offset)
        (if (nil? tail)
          args
          (recur args tail)))
      :set
      (let [[bft-kw offset value & tail] tail]
        (.set args (bft bft-kw) offset value)
        (if (nil? tail)
          args
          (recur args tail)))
      :incrby
      (let [[bft-kw offset amount & tail] tail]
        (.incrBy args (bft bft-kw) offset amount)
        (if (nil? tail)
          args
          (recur args tail))))))
