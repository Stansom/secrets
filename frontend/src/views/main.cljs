(ns views.main
  (:require
   [routing :as routes]))

(defn selector []
  (fn []
    [:div.section.is-medium
     [:div.columns.is-centered.is-vcentered.is-mobile
      [:div.column.is-narrow.has-text-centered.is-two-thirds
       [:div.block
        [:h1.is-size-3 "To manage your passwords: login or create a new master password"]]
       [:div.buttons.mt-2.is-centered
        [:button.button.is-link {:on-click #(routes/push-state! "/login")} "Login"]
        [:button.button.is-link {:on-click #(routes/push-state! "/create")} "Create"]]]]]))

