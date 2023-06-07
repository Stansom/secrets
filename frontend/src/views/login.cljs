(ns views.login
  (:require
   [components.input :as inp]
   [http.requests :as http]
   [reagent.core :as r]
   [routing :as routes]
   [storage.db :as db]))

(defn login []
  (let [in (r/atom "")]
    (fn []
      [:div.columns.is-mobile.is-centered
       [:div.column.is-three-quarters.is-centered.has-text-centered
        (when @(db/subscribe :not-authorized)
          [:div.notification.is-danger {:style {:margin-top "10px"}}
           "WRONG PASSWORD"])
        [:h1.block.is-narrow.is-size-2 "Login"]
        [:form.login__master_form.control.has-icons-left
         {:on-submit
          #(do
             (-> % .preventDefault)
             (http/login in))}

         [:h2.is-size-4 "Enter your master password here:"]
         (inp/input {:class "input"
                     :type "password"
                     :value @in
                     :placeholder "Enter password here"} in)
         [:button.button.is-info.mt-2.main__master_send {:type "submit"} "Login"]]
        [:div.or_register.mt-5
         [:h5.block.is-narrow.is-size-5 "Or create a new password"]
         [:button.button.is-success
          {:on-click #(routes/push-state! "/create")} "Create"]]]])))

