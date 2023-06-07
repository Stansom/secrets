(ns views.passwords-list
  (:require
   [components.add-modal :as add]
   [components.password-entry :as pe]
   [http.requests :refer [req]]
   [storage.db :as db]
   [routing :as routes]
   [components.icons :as icons]))

(defn listing []
  (let [_ (req)]
    (fn []
      (let [passwords (db/subscribe :passwords)
            saved? (db/subscribe :entry-saved?)
            not-authorized? (db/subscribe :not-authorized)]
        [:div.section.has-text-centered
         (when @not-authorized?
           [:div.already-logged.notification.is-primary.mt-5
            [:h4.is-size-4 "You're not authorized"]
            [:button.button.is-success.mt-2 {:on-click #(routes/push-state! "/login")} "Login"]])
         (when @saved? [:div.notification.is-primary "saved"])
         (when @(db/subscribe :add-modal?)
           [add/add])
         [:h1.title [:strong "SECRETS"]]
         [:button.button.is-info {:class (when (nil? @passwords) "is-loading")
                                  :style {:position "relative"
                                          :top "0"
                                          :right "-45%"
                                          :border-radius "50%"}
                                  :on-click #(do (db/set-value! :add-modal? true))}
          [:span {:style {:opacity (if-not (nil? @passwords) 1 0)}}
           [icons/plus]]]
         [:div {:style {:display "grid"
                        :grid-template-columns "30% 30% 30% 60px"
                        :justify-items "start"
                        :margin-left "5%"
                        :margin-bottom "5px"}}
          [:span.is-size-4 "URL"]
          [:span.is-size-4 "Login"]
          [:span.is-size-4 "Password"]]

         [:ul.is-flex.is-flex-direction-column
          (when (seq @passwords)
            (doall (map (fn [[id entry]]
                          ^{:key id}
                          [pe/pass-entry entry]) @passwords)))]]))))
