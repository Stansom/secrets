(ns views.not-found
  (:require
   [routing :as routes]))

(defn not-found []
  (fn []
    [:div.has-text-centered [:h1.is-size-2 "Not found"]
     [:h2.is-size-4 "Back to Home?"]
     [:button.button {:on-click #(routes/push-state! "/")} "Yes"]]))