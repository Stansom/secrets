(ns http.requests
  (:require
   [app.common.config :as config]
   [cljs-http.client :as http]
   [cljs.core.async :as async]
   [json.json :as json]
   [routing :as routes]
   [storage.db :as db]
   [ws.ws :as websocket]))

;; (declare req)

(def ws (websocket/web-socket "ws://localhost:8080"))

(comment
  ws
  (websocket/send @ws (json/stringify {:a 1})))
  ;;

(def request-handlers
  {:get-passwords {200 (fn [ps] (db/set-passwords! (json/parse (ps :body)))
                         (db/set-value! :not-authorized false))
                   401 (fn [] (db/set-value! :not-authorized true))}

   :login {200 #(do (routes/push-state! "/list")
                    (db/set-value! :not-authorized false))
           401 #(do (db/set-value! :not-authorized true))}

   :update {200 #(do (db/set-value! :entry-saved? true)
                     (db/set-passwords! (json/parse (% :body)))
                     (js/setTimeout
                      (fn []
                        (db/set-value! :entry-saved? false)) 1000) #_#_let [saved? (db/subscribe :entry-saved?)])}

   :remove {200 #(do
                   (db/set-value! :delete-modal? false)
                   (db/set-passwords! (json/parse (% :body))))}
   :add {200 #(let [saved? (db/subscribe :entry-saved?)]
                (db/set-value! :entry-saved? true)
                (db/set-passwords! (json/parse (% :body)))
                (js/setTimeout
                 (fn []
                   (db/set-value! :entry-saved? false)) 500))}

   :create-password {200 #(case (:body %)
                            "password has been added" (routes/push-state! "/list")
                            "already logged" (db/set-value! :already-logged? true))}})

(defn handle [#_m h r]
  (get (request-handlers h) r
       (constantly "no req handler")))

(defn get-cookies [] (. js/document -cookie))

(defn req []
  (-> (websocket/wait-for-open ws)
      (.then #(websocket/send ws (json/stringify
                                  {:uri "/list" :request-method :get
                                   :cookies (get-cookies)}))))

  (websocket/on-message ws
                        #(let [resp (json/parse (-> % .-data))
                               handler (handle :get-passwords (:status resp))]
                           (handler resp))))

(defn remove-entry []
  (websocket/send ws
                  (json/stringify
                   {:uri "/remove-pass" :request-method :delete
                    :body (str "id=" @(db/subscribe :clicked-entry-id))
                    :cookies (get-cookies)}))

  (websocket/on-message ws
                        #(let [resp (json/parse (-> % .-data))
                               handler (handle :remove (:status resp))]
                           (handler resp))))

(defn update-entry [login-inp pass-inp url-inp id]
  (websocket/send ws
                  (json/stringify
                   {:uri "/update-entry" :request-method :put
                    :body (str "login=" @login-inp "&" "url=" @url-inp "&" "password=" @pass-inp)
                    :cookies (get-cookies)}))

  (websocket/on-message ws
                        #(let [resp (json/parse (-> % .-data))
                               handler (handle :update (:status resp))]
                           (handler resp))))

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
                    :cookies (get-cookies)}))

  (websocket/on-message ws
                        #(let [resp (json/parse (-> % .-data))
                               handler (handle :add (:status resp))]
                           (handler resp))))

(defn create-password [in]
  (async/go
    (let [r (async/<! (http/post (str (config/host :server) "/set-password")
                                 {:with-credentials? true
                                  :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                  :form-params {:password @in}}))
          handler (handle :create-password (r :status))]
      (handler r))))
