(ns storage.db
  (:require
   [reagent.core :as r]))

(defonce db (r/atom {:passwords nil
                     :clicked-entry-id nil
                     :clicked-url nil
                     :clicked-pass nil
                     :clicked-login nil
                     :entry-removed? false
                     :entry-saved? false
                     :delete-modal? false
                     :edit-modal? false
                     :add-modal? false
                     :not-authorized nil
                     :already-logged? false}))

(defn retrieve-entry [id]
  (r/cursor db [:passwords id]))

(defn update-entry [c k v]
  (swap! c assoc k v))

(defn set-passwords! [ps]
  (swap! db assoc :passwords
         (into (sorted-map) (map (juxt :id identity) ps))))

(defn set-value! [key val]
  (swap! db assoc key val))

(defn toggle-edit-modal []
  (swap! db assoc :edit-modal? (not (@db :edit-modal?))))

(defn subscribe [subj]
  (r/cursor db [subj]))

(comment
  (apply str (map (constantly "•") "helloMaan111"))
  (set-value! :clicked-entry-id "id")
  (:delete-modal? @db)
  (:entry-removed? @db)
  (:clicked-entry-id @db)
  (:passwords @db)
  (:add-modal? @db)
  (set-value! :delete-modal? false)
  @db
  (reset! db nil)
  (swap! db conj "123")
  ;;
  )
