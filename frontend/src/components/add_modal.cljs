(ns components.add-modal
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

(defn add []
  (let [close-modal #(db/set-value! :add-modal? false)
        add-btn-enabled (r/atom false)
        login-inp (r/atom "")
        url-inp (r/atom "")
        pass-inp (r/atom "")
        reset-inputs (fn [] (reset! login-inp "") (reset! url-inp "") (reset! pass-inp ""))
        push-to-server #(http/add-entry login-inp pass-inp url-inp)
        key-handler #(case %
                       "Enter" (push-to-server)
                       "Escape" (close-modal)
                       "Esc"  (close-modal)
                       nil)
        _ (.addEventListener js/document "keyup" #(key-handler (. % -key)))]
    (fn []
      (let [_ (when @(db/subscribe :entry-saved?) (reset-inputs) (close-modal))]

        [:div.modal.is-four-fifths {:class (when @(db/subscribe :add-modal?) "is-active")}
         [:div.modal-background {:on-click close-modal}]
         [:div.modal-content
          [:div.box
           [:div.mb-5.is-size-3 "Add a new entry"]
           [:form.entry_edit_form
            {:on-submit #(-> % .preventDefault)}
            [:section.media
             [:div.media-content
              [:div.content
               [:div.url.level.is-mobile
                [:div.level-left item-style
                 [:label.level-item "URL:"]
                 [i/input {:class "level-item"
                           :value @url-inp} url-inp]]]
               [:div.login.level.is-mobile
                [:div.level-left item-style
                 [:label.level-item "Login:"]
                 [i/input {:auto-focus true
                           :class "level-item"
                           :value @login-inp} login-inp]]]

               [:div.password.level.is-mobile
                [:div.level-left pass-style
                 [:label.level-item "Password:"]
                 [i/input {:class "level-item"
                           :value @pass-inp} pass-inp]
                 (-> (password-input @pass-inp)
                     (result/match
                      (fn [_] (reset! add-btn-enabled true) "")
                       (fn [r] (reset! add-btn-enabled false)
                         [:div.message.is-danger.is-small [:div#password_error.message-body r]])))]]]

              [:div.level.is-mobile.is-justify-content-flex-end
               [:div.level-right
                [:button.button.level-item.is-danger.is-small {:aria-label "cancel"
                                                               :on-click close-modal} "Cancel"]
                [:button.button.level-item.is-success.is-small.ml-1 {:disabled (and (not @add-btn-enabled))
                                                                     :on-click push-to-server
                                                                     :aria-label "add"} "Add"]]]]]]]]
         [:button.modal-close.is-large {:aria-label "close"
                                        :on-click close-modal}]]))))
