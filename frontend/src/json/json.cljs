(ns json.json)

(defn stringify [c]
  (-> (clj->js c) js/JSON.stringify))

(defn parse [c]
  (-> (js/JSON.parse c) (js->clj {:keywordize-keys true})))

(comment
  (def a (stringify {:a :v}))
  a
  (js/JSON.parse a)
  (parse a)
;;
  )
