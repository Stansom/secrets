(ns ws-server
  (:require [babashka.json :as json]
            [org.httpkit.server :as server]
            [system :as system]))

(def clients (atom {}))

(defn ws-handler [request router]
  (server/as-channel
   request
   {:on-receive (fn [_ message]
                  (let [ps (json/read-str message {:key-fn keyword})
                        ps (-> ps
                               (update :request-method keyword)
                               (update :body #(into-array Byte/TYPE %)))
                        resp (router ps)]
                    (doseq [[_ cl] @clients]
                      (server/send! cl (json/write-str resp)))))
    :on-close   (fn [ch _]
                  (swap! clients dissoc (str ch))
                  #_(println "on-close:"   status))
    :on-open    (fn [ch]
                  (swap! clients assoc (str ch) ch)
                  #_(println "on-open:"    ch))}))

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
