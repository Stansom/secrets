(ns app.common.config
  (:require
   [app.common.result :as result]))

(def config
  {:server {:host "localhost"
            :port 2525}
   :front {:host "localhost"
           :port 5000}
   :static {:host "localhost"
            :port 5001}
   :db {:dbtype   "sqlite"
        :host     "localhost"
        :dbname   "passwords.db"
        :port     5432}})

(def validators
  {:password-input {:is-string (fn [s] (if (string? s) (result/ok s)
                                           (result/err "the value is not a string")))
                    :min-chars (fn [s]
                                 (if (< 8 (count s))
                                   (result/ok s)
                                   (result/err (str "password must contain min " 8 " characters"))))
                    :capitals-count (fn [s]
                                      (let [c (count (re-seq #"[A-Z]" s))]
                                        (if (<= 2 c)
                                          (result/ok s)
                                          (result/err
                                           (str "password must contain at least " 2 " capital letters")))))}})

(defn host [entry]
  (let [{{:keys [host port]} entry} config]
    (str "http://" host ":" port)))

(def front-host (host :static))
front-host
