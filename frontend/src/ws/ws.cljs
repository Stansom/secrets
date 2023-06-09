(ns ws.ws
  (:require [json.json :as json]))

(defn web-socket [url]
  (js/WebSocket. url))

(defn send [ws msg]
  (.send ws msg))

(defn close
  ([ws code reason]
   (.close ws code reason))
  ([ws code]
   (.close ws code))
  ([ws]
   (.close ws)))

(defn on-close [ws ev]
  (set! (. ws -onclose) ev))

(defn on-message [ws ev]
  (set! (. ws -onmessage) ev))

(defn on-error [ws ev]
  (set! (. ws -onerror) ev))

(defn on-open [ws ev]
  (set! (. ws -onopen) ev))

(defn wait-for-open
  [socket]
  (js/Promise.
   (fn [resolve]
     (if (not= (.-readyState socket) (.-OPEN socket))
       (on-open socket (fn [_] (resolve)))
       (resolve)))))

(comment
  (def ws (web-socket "ws://localhost:8080"))
  (js/alert ws)
  (on-message ws #(println "got a message" (-> % .-data)))
  (send ws (json/stringify {:a :b}))
  (close ws 1000 "deeead")
  ;;
  )
