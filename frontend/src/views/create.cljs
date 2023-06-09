(ns views.create
  (:require
   [components.input :as i]
   [components.logged-notification :as logged-notification]
   [http.requests :as http]
   [reagent.core :as r]
   [routing :as routes]
   [storage.db :as db]))

(defn create []
  (let [in (r/atom "")
        logged? (db/subscribe :already-logged?)]
    (fn []
      [:div.columns.is-mobile.is-centered
       [:div.create_password.column.is-three-quarters.is-centered.has-text-centered
        [:div.block.create_text.is-narrow
         (when @logged?
           [logged-notification/notification #(routes/push-state! "/list")])
         [:h1.is-size-2 "Create password"]
         [:form.control.has-icons-left
          {:on-submit #(do (-> % .preventDefault)
                           (http/create-password in))}
          [i/input {:class "input"
                    :type "password"
                    :value @in
                    :placeholder "Password"} in]
          [:span.icon.is-small.is-left [:i.fas.fa-lock]]
          [:button.button.is-info.mt-2 {:type "submit"}
           "create"]]]]])))
