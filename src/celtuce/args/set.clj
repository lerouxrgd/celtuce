(ns celtuce.args.set
  (:import 
   (com.lambdaworks.redis SetArgs)))

(defn set-args [& {ex :ex px :px nx :nx xx :xx}]
  (cond-> (SetArgs.)
    ex (.ex ^long ex)
    px (.px ^long px)
    nx (.nx)
    xx (.xx)))
