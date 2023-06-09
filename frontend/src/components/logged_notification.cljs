(ns components.logged-notification)

(defn notification [on-click]
  [:div.already-logged.notification.is-primary.mt-5
   [:h4.is-size-4 "You're already logged"]
   [:button.button.is-success.mt-2 {:on-click on-click} "Show passwords"]])
