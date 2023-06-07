(ns routing
  (:require
   [reagent.core :as r]))

(defonce path (r/atom ""))

(defn push-state! [p]
  (reset! path p)
  (-> js/window
      .-history
      (.pushState nil "" p)))

(defn set-path! [path]
  (reset! path (-> js/window .-location .-hash)))

(defn hook-browser-navigation
  "gets user interaction with browser navigation controls"
  []
  (.addEventListener js/window "popstate" #(let [p (-> js/window .-location .-pathname)]
                                             (reset! path p))))
