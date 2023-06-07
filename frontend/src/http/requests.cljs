(ns http.requests
  (:require
   [app.common.config :as config]
   [cljs-http.client :as http]
   [cljs.core.async :as async]
   [routing :as routes]
   [storage.db :as db]))

;; TODO: WS support
;; Display login window if not authorized while listing passwords

(declare req)

(def request-handlers
  {:get-passwords {200 (fn [ps] (db/set-passwords! (ps :body))
                         (db/set-value! :not-authorized false))
                   401 (fn [ps] (db/set-value! :not-authorized true))}

   :login {200 #(do (routes/push-state! "/list")
                    (db/set-value! :not-authorized false))
           401 #(do (db/set-value! :not-authorized true)

                    #_(js/setTimeout
                       (fn [] (db/set-value! :not-authorized false)) 3000))}
   :update {200 #(let [saved? (db/subscribe :entry-saved?)]
                   (db/set-value! :entry-saved? true)
                   (js/setTimeout
                    (fn []
                      (db/set-value! :entry-saved? false)) 1000)
                   (when @saved? (req)))}
   :remove {200 #(do
                   (req)
                   (db/set-value! :delete-modal? false))}
   :add {200 #(let [saved? (db/subscribe :entry-saved?)]
                (db/set-value! :entry-saved? true)
                (js/setTimeout
                 (fn []
                   (db/set-value! :entry-saved? false)) 500)
                (when @saved? (req)))}
   :create-password {200 #(case (:body %)
                            "password has been added" (routes/push-state! "/list")
                            "already logged" (db/set-value! :already-logged? true))}

;;
   })

(defn handle [#_m h r]
  (get (request-handlers h) r
       (constantly "no req handler")))

(defn req []
  (async/go
    (let [ps (async/<!
              (http/get
               (str (config/host :server) "/list")
               {:channel (async/chan 1)
                :with-credentials? true
                :headers {"Accept" "application/json"}}))
          handler (handle :get-passwords (ps :status))]
      (handler ps))))

(defn remove-entry []
  (let [ch (async/chan 1)]
    (async/go (async/>! ch
                        (async/<! (http/delete (str (config/host :server) "/remove-pass")
                                               {:with-credentials? true
                                                :headers {"Accept" "text/plain"}
                                                :form-params {:id @(db/subscribe :clicked-entry-id)}}))))

    (async/go-loop []
      (when-let [d (async/<! ch)]
        (let [handler (handle :remove (d :status))]
          (handler d)))

      (recur))))

(defn update-entry [login-inp pass-inp url-inp id]
  (async/go
    (let [r (async/<!
             (http/put (str (config/host :server) "/update-entry")
                       {:with-credentials? true
                        :headers {"Accept" "text/plain"}
                        :form-params {:login @login-inp :password @pass-inp :url @url-inp :id @id}}))

          handler (handle :update (r :status))]

      (handler r))))

(defn login [in]
  (async/go
    (let [r (async/<! (http/post (str (config/host :server) "/login")
                                 {:with-credentials? true
                                  :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                  :form-params {:password @in}}))
          handler (handle :login (r :status))]
      (handler r))))

(defn add-entry [login-inp pass-inp url-inp]
  (async/go
    (let [r (async/<!
             (http/put (str (config/host :server) "/add")
                       {:with-credentials? true
                        :headers {"Accept" "text/plain"}
                        :form-params {:login @login-inp :password @pass-inp :url @url-inp}}))

          handler (handle :add (r :status))]
      (handler r))))

(defn create-password [in]
  (async/go
    (let [r (async/<! (http/post (str (config/host :server) "/set-password")
                                 {:with-credentials? true
                                  :headers {"Content-Type" "application/x-www-form-urlencoded"}
                                  :form-params {:password @in}}))
          handler (handle :create-password (r :status))]
      (handler r))))
