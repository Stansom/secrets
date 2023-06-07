(ns queries
  (:require [clojure.string :as str]))

(defn parse-query
  "Parse uname=name to [uname name]"
  [q]
  (let [[qry param] (str/split q #"=")
        param (when param (first (str/split param #";")))]
    [qry param]))

(defn parse-multiple-query
  "Parse uname=name&upass=pass1 to {:uname \"name\" :upass \"pass1\"}"
  [q]
  (if-not (empty? q)
    (let [s (str/split q #"&")]
      {:ok (reduce (fn [acc st]
                     (let [[k v] (parse-query st)]
                       (assoc acc (keyword k) v))) {} s)})
    {:err {:info "can't parse query"}}))

(defn check-pers
  "Checks that keys (ks) are presented and if all good
   calls function (f)"
  [ks f]
  (if (and
       (seq ks)
       (every? seq ks))
    (f)
    {:err {:info "all keys must be presented"}}))
