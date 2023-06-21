(ns http.requests
  (:require
   [app.common.config :as config]
   [cljs-http.client :as http]
   [cljs.core.async :as async]
   [json.json :as json]
   [routing :as routes]
   [storage.db :as db]
   [ws.ws :as websocket]))

(def ws (websocket/web-socket "ws://localhost:8080"))

(def request-handlers
  {:list-passwords {200 (fn [body] (db/set-passwords! (json/parse body))
                          (db/set-value! :not-authorized false))
                    401 (fn [] (db/set-value! :not-authorized true))}

   :login {200 #(do (routes/push-state! "/list")
                    (db/set-value! :not-authorized false))
           401 #(do (db/set-value! :not-authorized true))}

   :update-entry {200 #(db/set-passwords! (json/parse %))}

   :remove-password {200 #(db/set-passwords! (json/parse %))}

   :add {200 #(db/set-passwords! (json/parse %))}

   :create-password {200 #(case (:body %)
                            "password has been added" (routes/push-state! "/list")
                            "already logged" (db/set-value! :already-logged? true))}})

(defn handle [h r]
  (get (request-handlers h) r
       (constantly "no req handler")))

(defn get-cookies [] (. js/document -cookie))

(defn msg-handler [msg]
  (let [{:keys [type body status]} (json/parse (-> msg .-data))
        type (keyword type)
        handler (handle type status)]
    (handler body)))

(websocket/on-message ws msg-handler)

(defn req []
  (-> (websocket/wait-for-open ws)
      (.then #(websocket/send ws (json/stringify
                                  {:uri "/list" :request-method :get
                                   :cookies (get-cookies)})))))
(defn remove-entry [id]
  (websocket/send ws
                  (json/stringify
                   {:uri "/remove-pass" :request-method :delete
                    :body (str "id=" @id)
                    :cookies (get-cookies)})))

(defn update-entry [login-inp pass-inp url-inp id]
  (websocket/send ws
                  (json/stringify
                   {:uri "/update-entry" :request-method :put
                    :body (str "login=" @login-inp "&" "url=" @url-inp "&" "password=" @pass-inp "&" "id=" @id)
                    :cookies (get-cookies)})))

(defn login [in]
  (async/go
    (let [r (async/<! (http/post (str (config/host :server) "/login")
                                 {:with-credentials? true
                                  :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                  :form-params {:password @in}}))
          handler (handle :login (r :status))]
      (handler r))))

(defn add-entry [login-inp pass-inp url-inp]
  (websocket/send ws
                  (json/stringify
                   {:uri "/add" :request-method :put
                    :body (str "login=" @login-inp "&" "url=" @url-inp "&" "password=" @pass-inp)
                    :cookies (get-cookies)})))

(defn create-password [in]
  (async/go
    (let [r (async/<! (http/post (str (config/host :server) "/set-password")
                                 {:with-credentials? true
                                  :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                  :form-params {:password @in}}))
          handler (handle :create-password (r :status))]
      (handler r))))
