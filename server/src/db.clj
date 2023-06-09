(ns db
  (:require
   [babashka.pods :as pods]
   [honey.sql :as sql]
   [encryption :as encryption]
   [app.common.config :as config]))

(def conf config/config)

(def db (or (:db conf) {:dbtype   "sqlite"
                        :host     "localhost"
                        :dbname   "passwords.db"
                        :port     5432}))

(pods/load-pod 'org.babashka/go-sqlite3 "0.1.0")
(require '[pod.babashka.go-sqlite3 :as sqlite])

(def create-passwords-table-query {:create-table [:passwords :if-not-exists]
                                   :with-columns [[:id :integer :primary-key]
                                                  [:url :text [:not nil]]
                                                  [:password :text [:not nil]]
                                                  [:login :text [:not nil]]]})

(defn create-db! [{:keys [:dbname]} qrs]
  (sqlite/execute! dbname (-> qrs
                              sql/format)))

(defn insert-into [{dbname :dbname} t opts]
  (let [{:keys [columns values]} opts]
    (sqlite/query dbname (-> {:insert-into t
                              :columns columns
                              :values [values]
                              :on-conflict {:do-nothing true}} (sql/format)))))
(defn update-table [{dbname :dbname} t k v clause]
  (sqlite/query dbname (-> {:update [t]
                            :set {k v}
                            :where [clause]} (sql/format))))

(defn select-from
  ([{dbname :dbname} qry]
   (sqlite/query dbname (-> qry (sql/format))))
  ([{dbname :dbname} t fs q]
   (sqlite/query dbname (-> {:select fs
                             :from [t]
                             :where [q]} (sql/format)))))

(defn delete-from [{dbname :dbname} t clause]
  (let [[f s th] clause
        dcl (if (= s :id) [f :rowid th] clause)]
    (sqlite/query dbname (-> {:delete-from [t]
                              :where [dcl]} (sql/format)))))

(defn insert-pass! [db url login pass]
  (if (and (pos? (count url)) (pos? (count pass)) (pos? (count login)))
    (let [hs (encryption/encrypt-pass pass)]
      (insert-into db
                   :passwords
                   {:columns [:url :password :login]
                    :values [url hs login]}))
    {:err :url-user-pass-is-empty}))

(defn find-password [db t u]
  (-> (select-from db {:select [:password]
                       :from :passwords
                       :where [:= :username u]
                       :limit 1})
      first
      :passwords/password))

(defn list-passwords [db]
  (map (fn [e]
         (let [ep (encryption/decrypt-pass (:password e))]
           {:password ep
            :url (:url e)
            :login (:login e)
            :id (:id e)}))
       (select-from db {:select [:*]
                        :from [:passwords]})))

(defn remove-password [db id]
  (delete-from db :passwords [:= :id id]))

#_(defn update-password [db v id]
    (update-table db :passwords :password (encryption/encrypt-pass v) [:= :id id]))

#_(defn update-url [db v id]
    (update-table db :passwords :url v [:= :id id]))

#_(defn update-login [db v id]
    (update-table db :passwords :login v [:= :id id]))

(defn update-entry [{dbname :dbname} url login password id]
  (sqlite/query dbname (-> {:update [:passwords]
                            :set {:password (encryption/encrypt-pass password)
                                  :url url
                                  :login login}
                            :where [:= :id id]} (sql/format))))

(comment
  (create-db! db create-passwords-table-query)
  (insert-pass! db "asd.com" "log1" "pass1")
  (insert-pass! db "asd123.coaa" "log2" "pass2")
  (list-passwords db)

  (update-entry {:dbname "passwords.db"} "u-r-l" "l-in" "p-ass" 1)

  (sqlite/query "passwords.db" (-> {:update [:passwords]
                                    :set {:password (encryption/encrypt-pass "new1")
                                          :url "new21.com"
                                          :login "bobin-new21"}
                                    :where [:= :id 1]} (sql/format)))


  (sqlite/query "passwords.db" (-> {:select :*
                                    :from :passwords} sql/format))
  ;;
  )
