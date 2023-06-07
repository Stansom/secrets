(ns auth
  (:require
   [clojure.string :as str]
   [date :as date]
   [encryption :as encryption]
   [queries :as query]
   [app.common.result :as result]
   [babashka.fs :as fs]))

(defn parse-post-body
  "Parse REQ body from input stream to string"
  [req]
  (when-not (nil? (:body req))
    (when-let [b (slurp (:body req))]
      b)))

(defn gen-cookie
  "Generates cookie string with key (k) and value (v), can take
   expire date as third argument (exp)"
  ([k v]
   (gen-cookie k v nil))
  ([k v exp]
   (str k "=" v (when exp (str "; Expires=" exp "; ")) "path=" "/" ";")))

#_(defn parse-req-cookie
    "Returns REQ cookies as a map
   \"cookie-1=val-1; cookie-2=val-2;\"
   {:cookie-1 val-1 :cookie-2 val-2}"
    [req]
    (let [spl (map str/trim (str/split (get-in req [:headers "cookie"]) #";"))
          parsed (into {} (map (comp (juxt (comp keyword str/lower-case first) second) query/parse-query) spl))]
      parsed))

(defn parse-cookie
  "Takes exact cookie (item) from cookie string"
  [c item]
  (when (seq c)
    (let [parsed (second (str/split c (re-pattern (str item "="))))]
      (when parsed
        (str/replace parsed ";" "")))))

(defn save-token-to-cookie
  "Takes token from REQ headers field and if there's no token in cookies
   sets the token to RESP map headers field"
  ([req res]
   (let [t (get-in req [:headers "token"])
         cookie-token (parse-cookie (get-in req [:headers "cookie"]) "token")]
     (if (and (seq t) (not (seq cookie-token)))
       (update-in res [:headers "Set-Cookie"] conj (gen-cookie "token" t
                                                               (date/date-to-utc (date/hours-to-date 24)))
                  #_{"Set-Cookie" (gen-cookie "token" t
                                              (date-to-utc (hours-to-date 24)))})
       res))))

#_(defn- wrap-username
    "Conjoin username to REQ map"
    [req user]
    (assoc req :username user))

#_(defn- wrap-password
    "Conjoin password to REQ map"
    [req pass]
    (assoc req :password pass))

(defn- wrap-authorized
  "Adds authorized field to REQ"
  [req]
  (assoc req :authorized true))

(defn persist-token-to-headers
  "Conjoin token to REQ header"
  [req t]
  (assoc-in req [:headers "token"] t))

(defn- persist-and-wrap-token
  "Encrypt token then save it to the Systems session tokens list
   and adds username field to REQ map"
  [req]
  (fn [pass]
    (let [encr-t (encryption/encrypt-pass pass)]
      (->  req
           (persist-token-to-headers encr-t)
           (wrap-authorized)))))

(defn- check-pass
  "Checks password"
  [pass]
  (if (fs/exists? "master.p")
    (let [p (slurp "master.p")
          de  (encryption/decrypt-pass p)]
      (result/of de #(= % pass)))
    {:err "can't read password from file"}))

(defn- auth-err-handler [req]
  {:payload (dissoc req :authorized)
   :info :not-authorized})

(defn auth-token
  "Tries to authorize by token wich located in headers"
  [req]
  (-> req :headers (get "token")
      (result/of #(seq %))
      (result/flat-map-ok (fn [t] (encryption/decrypt-token t)))
      (result/of #(seq (second %)))
      (result/flat-map-ok
       #(check-pass (second %)))
      (result/map-ok (persist-and-wrap-token req))
      (result/map-err (constantly (auth-err-handler req)))))

(defn auth-up
  "Tries to authorize by user/password presented in the query-string"
  [req]
  (-> req :query-string (query/parse-multiple-query)
      (result/flat-map-ok (fn [r] (result/of r #(and (:username %) (:password %)))))
      (result/flat-map-ok
       #(check-pass (:password %)))
      (result/map-ok (persist-and-wrap-token req))
      (result/map-err (constantly (auth-err-handler req)))))

(defn auth-body
  "Tries to authorize REQ bodys user/password fields"
  [req]
  (-> req parse-post-body
      (result/of #(seq %))
      (result/flat-map-ok query/parse-multiple-query)
      (result/flat-map-ok (fn [r] (-> r (result/of #(:password %)))))
      (result/flat-map-ok
       #(check-pass (:password %)))
      (result/map-ok (persist-and-wrap-token req))
      (result/map-err (constantly (auth-err-handler req)))))

#_(defn check-pass-file []
    (->
     (slurp "master.p")
     encryption/decrypt-token
     second))

#_(defn tap [x]
    (println "tapped>" x)
    x)

(defn already-logged?
  "Checks if user already logged-in via cookies and session token comparing"
  [req]
  (->
   (result/of (fs/exists? "master.p") true? #_#(and (true? %)))
   (result/flat-map-ok
    (fn [_] (-> req :headers (get "cookie") (parse-cookie "token")

                (result/of #(seq %))
                (result/flat-map-ok  #(-> (and (= (slurp "master.p") %) %)
                                          (result/of string?)))
                (result/map-ok #(-> (persist-token-to-headers req %))))))
   (result/map-err (constantly (auth-err-handler req)))))

#_(defn auth?
    "Pass REQ thru all authorization functions, then returns
   authorized REQ if the tries was succefful otherwise error map"
    [req]
    (-> req auth-token
        (result/flat-map-err (comp auth-up :payload))
        (result/flat-map-err (comp auth-body :payload))
        (result/map-err (constantly (auth-err-handler req)))))
