(ns clj-lettuce.args.scripting
  (:import 
   (com.lambdaworks.redis ScriptOutputType)))

(defn ^ScriptOutputType output-type [t]
  (case t
    :boolean ScriptOutputType/BOOLEAN
    :integer ScriptOutputType/INTEGER
    :mulit   ScriptOutputType/MULTI
    :status  ScriptOutputType/STATUS
    :value   ScriptOutputType/VALUE
    (throw (ex-info "invalid scripting output type" {:type t}))))
