(ns ws-server
  (:require [babashka.json :as json]
            [org.httpkit.server :as server]
            [system :as system]))

(defn ws-handler [request router]
  (server/as-channel
   request
   {:on-receive (fn [ch message]
                  (let [ps (json/read-str message {:key-fn keyword})
                        ps (-> ps
                               (update :request-method keyword)
                               (update :body #(into-array Byte/TYPE %))
                               #_(assoc :params {:token (:token ps)}))
                        resp (router ps)
                        resp (into {} (filter (comp #{:status :body #_:token} key) resp))]
                    (server/send! ch (json/write-str resp))
                    (println "on-receive:" ps "request:" (:uri request))))
    :on-close   (fn [ch status]  (println "on-close:"   status))
    :on-open    (fn [ch] (println "on-open:"    ch))}))

(defn ws-server [port router]
  (let [s (server/run-server #(ws-handler % router) {:port port})]
    (println "Starting WS server on port:" port)
    (swap! system/system assoc :ws-server s :port port)))

(defn stop-ws []
  (when-some [s (:ws-server @system/system)]
    (s)
    (swap! system/system assoc :server nil)))

(comment
  (stop-ws)
  ;;
  )
