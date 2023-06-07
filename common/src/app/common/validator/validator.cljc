(ns  app.common.validator.validator
  (:require
   [app.common.result :as result]
   [app.common.config :as config]))

(defn pipe [validator val]
  (let [[ffn & rfns] validator]
    (reduce (fn [acc f] (result/flat-map-ok acc f))
            (ffn val) rfns)))

(comment
  (pipe (vals (config/validators :password-input)) "asdasdasdasdAA")
  ;;
  )
