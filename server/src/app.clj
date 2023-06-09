(ns app
  (:refer-clojure :exclude [list])
  (:require [app.common.config :as config]
            [clojure.pprint :as pprint]
            [clojure.tools.cli :refer [parse-opts]]
            [handlers :as hs]
            [org.httpkit.server :as server]
            [routes :as rb]
            [ruuter.core :as ruuter]
            [static-server :as static]
            [system :as system]))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port"
    :default (-> config/config :server :port)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 65535)  "Must be a number from 0 to 65535"]]
   ["-sp" "--sport SPORT" "Static Port"
    :default (-> config/config :static :port)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 65535)  "Must be a number from 0 to 65535"]]])

(def routes
  [{:path "/logout"
    :methods {:get (fn [req] (hs/dispatch {:type :logout :payload req}))}}

   {:path "/register"
    :methods {:post (fn [req] (hs/dispatch {:type :register :payload req}))}}

   {:path "/login"
    :methods {:post (fn [req] (hs/dispatch {:type :login-post :payload req}))}}

   {:path "/list"
    :methods {:get (fn [req] (hs/dispatch {:type :list-passwords :payload req}))
              :options (fn [req] (hs/dispatch {:type :allow-cors :payload req}))}}

   {:path "/random"
    :methods {:get (fn [req] (hs/dispatch {:type :random-password :payload req}))}}

   {:path "/test"
    :methods {:get (fn [req] {:status 200
                              :body (with-out-str (pprint/pprint req))})}}

   {:path "/add"
    :methods {:put (fn [req] (hs/dispatch {:type :add :payload req}))
              :options (fn [req] (hs/dispatch {:type :allow-cors :payload req}))}}

   {:path "/update-entry"
    :methods {:put (fn [req] (hs/dispatch {:type :update-entry :payload req}))
              :options (fn [req] (hs/dispatch {:type :allow-cors :payload req}))}}

   {:path "/remove-pass"
    :methods {:delete (fn [req] (hs/dispatch {:type :remove-password :payload req}))
              :options (fn [req] (hs/dispatch {:type :allow-cors :payload req}))}}

   {:path "/set-password"
    :methods {:post (fn [req] (hs/dispatch {:type :set-password :payload req}))
              :options (fn [req] (hs/dispatch {:type :allow-cors :payload req}))}}])

(defn app [req]
  (ruuter/route (rb/routes-builder routes) req))

(defn start-server [port]
  (let [s (server/run-server #'app
                             {:port port})]
    (println "Starting server on port:" port)
    (hs/dispatch {:type :create-db! :payload nil})
    (swap! system/system assoc :server s :port port)))

(defn stop-serve []
  (when-some [s (:server @system/system)]
    (s)
    (swap! system/system assoc :server nil)))

(defn run-server [port st-port]
  (start-server port)
  (static/start-static-server st-port))

(defn stop-server []
  (stop-serve)
  (static/stop-static-server))

(defn restart-server []
  (stop-server)
  (run-server (:port @system/system)
              (:static-port @system/system)))

(defn -main [& args]
  (let [{:keys [_ options]} (parse-opts args cli-options)
        {:keys [port sport]} options]
    (cond
      port (do (run-server port sport) @(promise))
      :else (println "nothing"))))
(comment
  (-main "--stop")
  (stop-server)
  (run-server 2525 5001)
  @system/system
  (restart-server)
  ;;
  )
