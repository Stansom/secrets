(ns components.password-entry
  (:require
   [components.delete-modal :as dm]
   [components.edit-modal :as ed-mod]
   [components.icons :as icons]
   [reagent.core :as r]
   [storage.db :as db]))

(defn pass-entry [{:keys [password id url login]}]
  (let [blurred? (r/atom true)
        copied? (r/atom false)
        remove-entry #(do (db/set-value! :clicked-entry-id id)
                          (db/set-value! :clicked-url url)
                          (db/set-value! :delete-modal? true))
        edit-entry #(do
                      (db/set-value! :clicked-entry-id id)
                      (db/set-value! :clicked-url url)
                      (db/set-value! :clicked-pass password)
                      (db/set-value! :clicked-login login)
                      (db/toggle-edit-modal))
        copy-to-clipboard #(do
                             (-> js/window .-navigator .-clipboard (.writeText %))
                             (reset! copied? true)
                             (js/setTimeout (fn [] (reset! copied? false)) 1000))]
    (fn [{:keys [password url login]}]
      (let [pass-inp (r/atom password)
            pass-hidden (r/atom (apply str (map (constantly "•") @pass-inp)) #_(str/replace @pass-inp #"." "*"))
            url-inp (r/atom url)
            login-inp (r/atom login)]
        [:li.column.password_entry {:class "box"}
         [dm/delete-modal]
         (when @(db/subscribe :edit-modal?)
           [ed-mod/modal])
         (when @copied? [:div.notification.is-success {:on-click #(reset! copied? false)}
                         [:button.delete {:on-click #(reset! copied? false)}] "password was copied"])
         [:div {:style {:display "grid"
                        :grid-template-columns "1fr 1fr 1fr 25px"
                        :justify-items "start"}}

          [:div @url-inp]
          [:div @login-inp]
          [:div {:style {:display "grid"
                         :grid-template-columns "minmax(3rem, 1fr) 60px"
                         :justify-items "start"
                         :min-width "85%"}}
           [:span {:style {:filter (if @blurred? "blur(4px)" "blur(0)")}}
            (if @blurred? @pass-hidden @pass-inp)]
           [:div.icons_box {:style {:display :flex
                                    :align-items :center
                                    :justify-content :space-evenly
                                    :width "100%"
                                    :height "100%"
                                    :z-index 1}}
            [icons/unlock blurred?]
            [icons/copy-to-cb #(copy-to-clipboard password)]]]
          [:div.edit_delete__icons {:style {:display :flex
                                            :align-items :center}}
           [:span.icon {:style {:width "19px" :height "19px"}
                        :on-click edit-entry}
            [icons/edit]]

           [:span.icon.pl-5 {:on-click remove-entry
                             :style {:color "#ce1c1c"}}
            [:i.fas.thin.fa-delete-left
             {:style {:z-index 1}}]]]]]))))
