(ns components.edit-modal
  (:require
   [app.common.result :as result]
   [components.input :as i]
   [http.requests :as http]
   [reagent.core :as r]
   [storage.db :as db]
   [app.common.validator.password :refer [password-input]]))

(def item-style {:style {:display "grid"
                         :grid-template-columns "100px auto"
                         :margin-bottom "5px"}})

(def pass-style {:style {:width "100%"
                         :display "grid"
                         :grid-template-columns "100px auto auto"
                         :margin-bottom "5px"}})

(defn modal
  "Edit entry modal window, 
   Args: takes `entry-data` map with keys [:login :url :password :id],
   `is-active` is atom to control the modal visibility
   `rf` is a function called when data was pushed to the server 
   `saved?` is atom to inform the world what data was successfully saved   
   "
  [id active?]
  (let [add-btn-enabled (r/atom true)
        switch-off #(reset! active? false)
        id (r/atom id)
        chosen-entry (db/retrieve-entry @id)
        login-inp (r/atom (@chosen-entry :login))
        url-inp (r/atom (@chosen-entry :url))
        pass-inp (r/atom (@chosen-entry :password))
        save-to-db #(do (db/set-value! :entry-saved? true)
                        (db/update-entry chosen-entry :login @login-inp)
                        (db/update-entry chosen-entry :url @url-inp)
                        (db/update-entry chosen-entry :password @pass-inp)
                        (js/setTimeout
                         (fn []
                           (db/set-value! :entry-saved? false)) 1000))
        save-handler #(do
                        (save-to-db)
                        (http/update-entry login-inp pass-inp url-inp id)
                        (switch-off))
        key-handler #(case %
                       "Enter" (save-handler)
                       "Escape" (switch-off)
                       "Esc" (switch-off)
                       nil)]
    (fn []
      [:div.modal.is-four-fifths.is-active
       [:div.modal-background {:on-click switch-off}]
       [:div.modal-content
        [:div.box
         [:div.mb-5.is-size-3 "Edit entry"]
         [:form.entry_edit_form
          {:on-submit #(-> % .preventDefault)}
          [:section.media
           [:div.media-content
            [:div.content
             [:div.url.level.is-mobile
              [:div.level-left item-style
               [:label.level-item "URL:"]
               [i/input {:class "level-item"
                         :on-key-down #(key-handler (. % -key))
                         :value @url-inp} url-inp]]]
             [:div.login.level.is-mobile
              [:div.level-left item-style
               [:label.level-item "Login:"]
               [i/input {:class "level-item"
                         :on-key-down #(key-handler (. % -key))
                         :value @login-inp} login-inp]]]
             [:div.password.level.is-mobile
              [:div.level-left pass-style
               [:label.level-item "Password:"]
               [i/input {:class "level-item"
                         :on-key-down #(key-handler (. % -key))
                         :value @pass-inp} pass-inp]
               (-> (password-input @pass-inp)
                   (result/match
                    (fn [_] (reset! add-btn-enabled true) "")

                     (fn [r] (reset! add-btn-enabled false)
                       [:div.message.is-danger.is-small [:div#password_error.message-body r]])))
               [:button.modal-close.is-large {:aria-label "close"
                                              :on-click switch-off}]]]
             [:div.level.is-mobile.is-justify-content-flex-end
              [:div.level-right
               [:button.button.level-item.is-danger.is-small {:aria-label "cancel"
                                                              :on-click switch-off} "Cancel"]
               [:button.button.level-item.is-success.is-small.ml-1 {:disabled (and (not @add-btn-enabled))
                                                                    :on-click save-handler
                                                                    :aria-label "save"} "Save"]]]]]]]]]])))
