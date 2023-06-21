(ns core
  (:require
   [reagent.dom.client :as rdomc]
   [routing :as rt]
   [ruuter.core :as ruuter]
   [views.create :as create]
   [views.login :as login]
   [views.main :as main]
   [views.not-found :as nf]
   [views.passwords-list :as l]))

(defonce root (rdomc/create-root (-> js/document (.getElementById "app"))))

(def routes
  [{:path "/"
    :response (fn [_] [main/selector])}
   {:path "/create"
    :response (fn [_] [create/create])}
   {:path "/index.html"
    :response (fn [_] [main/selector])}
   {:path "/login"
    :response (fn [_] [login/login])}
   {:path "/list"
    :response (fn [_] [l/listing])}])

(defn main []
  (let [_ #(rt/set-path! rt/path)
        init-loc (-> js/window .-location .-pathname)]
    (fn []
      (let [r (ruuter/route routes {:uri (if (seq @rt/path) @rt/path init-loc)})]
        (if (= 404 (:status r))
          [nf/not-found]
          r)))))

(defn ^:export ^:dev/after-load run []
  (rt/hook-browser-navigation)
  (rdomc/render root [main]))
