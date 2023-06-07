(ns handlers
  (:require [app.common.result :as result]
            [auth :as auth]
            [babashka.json :as json]
            [app.common.config :as config]
            [db :as db]
            [encryption :as encryption]
            [passgen :as gen]
            [queries :as query]
            [system :as system]))

;; (def front-host (config/host :static))
(def front-host config/front-host)

(def not-authorized-resp {:status 401 :headers {"Content-Type" "text/plain"}
                          :body (str :not-authorized)})

(defn list-passwords
  "Handles list passwords path, returns json"
  [req]
  (-> req
      (result/flat-map-ok (constantly {:status 200
                                       :headers {"Content-Type" "application/json"}
                                       :body (json/write-str
                                              (db/list-passwords db/db))}))
      (result/flat-map-err (constantly not-authorized-resp))))

(defn add-entry
  "Handles new password entry path"
  [req]
  (-> req
      (result/map-ok (fn [r] (-> r :body)))
      (result/flat-map-ok (fn [r] (result/of (slurp r) #(seq %))))
      (result/flat-map-ok (fn [r] (query/parse-multiple-query r)))
      (result/flat-map-ok (fn [r] (result/of r #(and (:url %) (:login %) (:password %)))))
      (result/flat-map-ok (fn [r] (db/insert-pass! db/db
                                                   (:url r)
                                                   (:login r)
                                                   (:password r))
                            {:status 200
                             :body (format "entry for URL %s was added" (:url r))}))

      (result/flat-map-err (constantly not-authorized-resp))))

(def random-password
  "Handles random password generation"
  (comp
   (partial assoc {:status 200} :body)
   (fnil gen/generate-random-symbols 6)
   (fnil parse-long "6") second (fnil query/parse-query "length=6") :query-string))

(defn remove-password
  "Handles password removing path"
  [req]
  (-> req
      (result/map-ok (fn [r] (-> r :body)))
      (result/flat-map-ok (fn [r] (result/of (slurp r) #(seq %))))
      (result/flat-map-ok (fn [r] (query/parse-multiple-query r)))
      (result/flat-map-ok (fn [r] (result/of r #(and (:id %)))))
      (result/flat-map-ok (fn [r] (db/remove-password db/db (parse-long (:id r)))
                            {:status 200
                             :body (format "entry with id %s was removed" (:id r))}))

      (result/flat-map-err (constantly not-authorized-resp))))

(defn logout
  "Handles logging out of the user"
  [req]
  (let [cookie-token (auth/parse-cookie (get-in req [:headers "cookie"]) "token")]
    (->
     cookie-token
     (result/of #(not (nil? %)))
     (result/flat-map-ok #(do
                            (system/remove-session-token! system/system %)
                            {:status 200
                             :body (str "User was successfully logged out")}))
     (result/flat-map-err (constantly {:err {:info "no user to log-out"}})))))

(defn login-post [req]
  (-> req
      (result/flat-map-ok
       (fn [r]
         (auth/save-token-to-cookie r {:status 200
                                       :body (str "User successfully authorized"
                                                  #_(encryption/decrypt-token (get-in r [:headers "token"])))})))
      (result/flat-map-err (constantly not-authorized-resp))))

(defn set-password [req]
  (when-let [[_ password] (-> req :body slurp query/parse-query)]
    (let [en (encryption/encrypt-pass password)
          _ (spit "master.p" en)
          r (auth/persist-token-to-headers req en)]
      (auth/save-token-to-cookie
       r {:status 200
          :body "password has been added"}))))

(defn update-entry [req]
  (-> req
      (result/map-ok (fn [r] (-> r :body)))
      (result/flat-map-ok (fn [r] (result/of (slurp r) #(seq %))))
      (result/flat-map-ok (fn [r] (query/parse-multiple-query r)))
      (result/flat-map-ok (fn [r] (result/of r #(and (:url %) (:login %) (:password %) (:id %)))))
      (result/flat-map-ok (fn [r] (db/update-entry db/db
                                                   (:url r)
                                                   (:login r)
                                                   (:password r)
                                                   (parse-long (:id r)))
                            {:status 200
                             :body (format "entry with id %s was updated" (:id r))}))

      (result/flat-map-err (constantly not-authorized-resp))))

(defn wrap-cors [res]
  (update res :headers merge {"Access-Control-Allow-Origin" front-host
                              "Access-Control-Allow-Credentials" "true"}))

(def handlers
  "Handlers map"
  {:add {:response #(-> % add-entry wrap-cors)
         :middleware [auth/already-logged?]}
   :update-entry {:response #(-> % update-entry wrap-cors)
                  :middleware [auth/already-logged?]}
   :allow-cors (constantly {:status 200
                            :headers {"Content-Type" "text/plain"
                                      "Access-Control-Allow-Methods"
                                      '("PUT"
                                        "DELETE"
                                        "POST")
                                      "Access-Control-Allow-Origin" front-host
                                      "Access-Control-Allow-Credentials" "true"}})
   :remove-password {:response #(-> % remove-password wrap-cors)
                     :middleware [auth/already-logged?]}
   :list-passwords {:response #(-> % list-passwords wrap-cors)
                    :middleware [auth/already-logged?]}
   :random-password random-password
   :set-password {:response #_#(-> % set-password wrap-cors)
                  (fn [r] (-> r
                              (result/flat-map-err
                               #(->> % :payload set-password
                                     (auth/save-token-to-cookie
                                      {:status 200
                                       :body "password has been added"})))
                              (result/flat-map-ok (constantly
                                                   (-> {:status 200
                                                        :body "already logged"}))) wrap-cors))
                  :middleware [auth/already-logged?]}
   :logout logout

   :login-post {:response #(->> % login-post wrap-cors)
                :middleware [#(-> % auth/already-logged?
                                  (result/flat-map-err (comp auth/auth-token :payload))
                                  (result/flat-map-err (comp auth/auth-body :payload)))]}
   :create-db! (fn [_] (db/create-db! db/db db/create-passwords-table-query))})



(defn dispatch
  "Dispatches routes, matching type over Handler map and calls 
   matched function with payload as argument, if there is 
   middleware presented then calls all middlewares functions
   on the payload data finally calls response function over
   all middleware functions 
   "
  [{:keys [type payload]}]
  (let [f (get handlers type)]
    (if (:middleware f)
      (let [fx (:response f)
            m (:middleware f)
            mws (reduce (fn [acc v] (v acc)) payload m)]
        (fx mws))
      (f payload))))

(comment
  (dispatch {:type :list-passwords :payload {:remote-addr "0:0:0:0:0:0:0:1",
                                             :params {},
                                             :headers
                                             {"accept-encoding" "identity",
                                              "host" "localhost:3030",
                                              "user-agent" "python-urllib3/1.26.10"
                                              "token" "8b5100074504c501f665b04879d0554700a6870d8c299d4590b0ea2c86aa2750"
                                              #_#_"Set-Cookie" '("username=b" "password=re")
                                              #_"cookie"
                                              #_"SLG_G_WPT_TO=ru; SLG_GWPT_Show_Hide_tmp=undefined; SLG_wptGlobTipTmp=undefined;"
                                              #_"SLG_G_WPT_TO=ru; 
                 SLG_GWPT_Show_Hide_tmp=undefined; 
                 SLG_wptGlobTipTmp=undefined; 
                 token=75e3a5fb5ad6ac70fc4a9a6f313ca9cec05189f4c607c5976cc08ff5c30124a1;"}
                                             :server-port 3030,
                                             :content-length 0,
                                             :websocket? false,
                                             :content-type nil,
                                             :character-encoding "utf8",
                                          ;;  :uri "/",
                                             :server-name "localhost",
                                             :query-string nil #_"username=koha&password=miloha&urlpassword=111new&url=111goo.com&login=jooh",
                                             :body (into-array Byte/TYPE "login=newlogn&url=newurl123&password=pass232323&id=1" #_"urlpassword=secret&url=oolo.com&login=jooh"),
                                             :scheme :http,
                                             :request-method :get}})

  (#(->> % login-post wrap-cors (auth/save-token-to-cookie (:ok %)))
   {:ok {:remote-addr "0:0:0:0:0:0:0:1",
         :params {},
         :headers
         {"accept-encoding" "identity",
          "host" "localhost:3030",
          "user-agent" "python-urllib3/1.26.10"
          "token" "8b5100074504c501f665b04879d0554700a6870d8c299d4590b0ea2c86aa2750"
          #_#_"Set-Cookie" '("username=b" "password=re")
          #_"cookie"
          #_"SLG_G_WPT_TO=ru; SLG_GWPT_Show_Hide_tmp=undefined; SLG_wptGlobTipTmp=undefined;"
          #_"SLG_G_WPT_TO=ru; 
                 SLG_GWPT_Show_Hide_tmp=undefined; 
                 SLG_wptGlobTipTmp=undefined; 
                 token=75e3a5fb5ad6ac70fc4a9a6f313ca9cec05189f4c607c5976cc08ff5c30124a1;"}
         :server-port 3030,
         :content-length 0,
         :websocket? false,
         :content-type nil,
         :character-encoding "utf8",
                                          ;;  :uri "/",
         :server-name "localhost",
         :query-string nil #_"username=koha&password=miloha&urlpassword=111new&url=111goo.com&login=jooh",
         :body (into-array Byte/TYPE "login=newlogn&url=newurl123&password=pass232323&id=1" #_"urlpassword=secret&url=oolo.com&login=jooh"),
         :scheme :http,
         :request-method :get}})

  (dispatch {:type :update-entry :payload {:remote-addr "0:0:0:0:0:0:0:1",
                                           :params {},
                                           :headers
                                           {"accept-encoding" "identity",
                                            "host" "localhost:3030",
                                            "user-agent" "python-urllib3/1.26.10"
                                            #_#_"token" "8b5100074504c501f665b04879d0554700a6870d8c299d4590b0ea2c86aa2750"
                                            #_#_"Set-Cookie" '("username=b" "password=re")
                                            "cookie"
                                            #_"SLG_G_WPT_TO=ru; SLG_GWPT_Show_Hide_tmp=undefined; SLG_wptGlobTipTmp=undefined;"
                                            "SLG_G_WPT_TO=ru; 
                 SLG_GWPT_Show_Hide_tmp=undefined; 
                 SLG_wptGlobTipTmp=undefined; 
                 token=75e3a5fb5ad6ac70fc4a9a6f313ca9cec05189f4c607c5976cc08ff5c30124a1;"}
                                           :server-port 3030,
                                           :content-length 0,
                                           :websocket? false,
                                           :content-type nil,
                                           :character-encoding "utf8",
                                          ;;  :uri "/",
                                           :server-name "localhost",
                                           :query-string nil #_"username=koha&password=miloha&urlpassword=111new&url=111goo.com&login=jooh",
                                           :body (into-array Byte/TYPE "login=newlogn&url=newurl123&password=pass232323&id=1" #_"urlpassword=secret&url=oolo.com&login=jooh"),
                                           :scheme :http,
                                           :request-method :get}})
  (update-entry {:ok {:remote-addr "0:0:0:0:0:0:0:1",
                      :params {},
                      :headers
                      {"accept-encoding" "identity",
                       "host" "localhost:3030",
                       "user-agent" "python-urllib3/1.26.10"
                       #_#_"token" "8b5100074504c501f665b04879d0554700a6870d8c299d4590b0ea2c86aa2750"
                       #_#_"Set-Cookie" '("username=b" "password=re")
                       "cookie"
                       #_"SLG_G_WPT_TO=ru; SLG_GWPT_Show_Hide_tmp=undefined; SLG_wptGlobTipTmp=undefined;"
                       "SLG_G_WPT_TO=ru; 
                 SLG_GWPT_Show_Hide_tmp=undefined; 
                 SLG_wptGlobTipTmp=undefined; 
                 token=75e3a5fb5ad6ac70fc4a9a6f313ca9cec05189f4c607c5976cc08ff5c30124a1;"}
                      :server-port 3030,
                      :content-length 0,
                      :websocket? false,
                      :content-type nil,
                      :character-encoding "utf8",
                                          ;;  :uri "/",
                      :server-name "localhost",
                      :query-string nil #_"username=koha&password=miloha&urlpassword=111new&url=111goo.com&login=jooh",
                      :body (into-array Byte/TYPE "login=newlogn&url=newurl123&password=pass232323&id=1" #_"urlpassword=secret&url=oolo.com&login=jooh"),
                      :scheme :http,
                      :request-method :get}})

  (dispatch {:type :list-passwords :payload {:remote-addr "0:0:0:0:0:0:0:1",
                                             :params {},
                                             :headers
                                             {"accept-encoding" "identity",
                                              "host" "localhost:3030",
                                              "user-agent" "python-urllib3/1.26.10"
                                              #_#_"token" "8b5100074504c501f665b04879d0554700a6870d8c299d4590b0ea2c86aa2750"
                                              #_#_"Set-Cookie" '("username=b" "password=re")
                                              "cookie"
                                              #_"SLG_G_WPT_TO=ru; SLG_GWPT_Show_Hide_tmp=undefined; SLG_wptGlobTipTmp=undefined;"
                                              "SLG_G_WPT_TO=ru; 
                 SLG_GWPT_Show_Hide_tmp=undefined; 
                 SLG_wptGlobTipTmp=undefined; 
                 token=75e3a5fb5ad6ac70fc4a9a6f313ca9cec05189f4c607c5976cc08ff5c30124a1;"}
                                             :server-port 3030,
                                             :content-length 0,
                                             :websocket? false,
                                             :content-type nil,
                                             :character-encoding "utf8",
                                          ;;  :uri "/",
                                             :server-name "localhost",
                                             :query-string nil #_"username=koha&password=miloha&urlpassword=111new&url=111goo.com&login=jooh",
                                             :body nil #_(into-array Byte/TYPE "password=newone" #_"urlpassword=secret&url=oolo.com&login=jooh"),
                                             :scheme :http,
                                             :request-method :get}})
  ;;
  )
