(ns components.delete-modal
  (:require
   [http.requests :as http]
   [storage.db :as db]))

(defn delete-modal []
  (let [close-modal #(db/set-value! :delete-modal? false)
        is-active (db/subscribe :delete-modal?)
        key-handler #(case %
                       "Escape" (close-modal)
                       "Esc"  (close-modal)
                       nil)
        _ (.addEventListener js/document "keyup" #(key-handler (. % -key)))]
    (fn []
      [:div.delete__modal
       [:div.modal.is-four-fifths {:class (when @is-active "is-active")}
        [:div.modal-background {:on-click close-modal}]
        [:div.modal-content
         [:div.box
          [:div.mb-5.is-size-3
           (str "Are you sure you want to remove the entry for URL: "
                @(db/subscribe :clicked-url) "?")]
          [:div.buttons {:style {:display :flex
                                 :width "100%" :height "100%"
                                 :align-items :flex-end
                                 :justify-content :space-evenly}}

           [:button.button.is-success {:on-click close-modal} "NO"]
           [:button.button.is-danger.is-small
            {:on-click http/remove-entry} "delete"]]]]]])))

