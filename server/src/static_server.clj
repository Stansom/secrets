(ns static-server
  (:require [app.common.result :as result]
            [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hiccup2.core :as html]
            [org.httpkit.server :as server]
            [system :as system])

  (:import [java.net URLEncoder]))

(def mimes (edn/read-string (slurp "src/mimes.edn")))

(def static-dir (fs/path "static"))

(assert (fs/directory? static-dir) "You don't have a directory with static files 
                                    to serve")

(defn file-ext [f]
  (when-let [ext (second (re-find #"\.(.{2,8}$)" f))]
    (str/lower-case ext)))

(defn ext->mime [f]
  (mimes (file-ext f)))

(ext->mime "pop.mp3")

(defn index [f]
  (let [files (map #(str (.relativize static-dir %))
                   (fs/list-dir f))]
    {:body (-> [:html
                [:head
                 [:meta {:charset "UTF-8"}]
                 [:title (str "Index of `" f "`")]]
                [:body
                 [:h1 "Index of " [:code (str f)]]
                 [:ul
                  (for [child files]
                    [:li [:a {:href (URLEncoder/encode (str child))} child
                          (when (fs/directory? (fs/path static-dir child)) "/")]])]
                 [:hr]
                 [:footer {:style {"text-aling" "center"}} "Served by http-server.clj"]]]
               html/html
               str)}))

(defn body [path]
  {:headers {"Content-Type" (ext->mime (fs/file-name path))}
   :body (fs/file path)})

(defn handle-statics [{:keys [:uri]}]
  (case uri
    "/js/main.js"
    (result/ok
     {:status 200
      :headers {"Content-Type" "text/javascript"}
      :body (fs/file (fs/path static-dir "js/main.js"))})

    "/css/main.css"
    (result/ok
     {:status 200
      :headers {"Content-Type" "text/css"}
      :body (fs/file (fs/path static-dir "css/main.css"))})

    (result/err uri)))

(defn start-static-server [port]
  (let [s (server/run-server
           (fn [{:keys [:uri] :as req}]
             (-> req handle-statics
                 (result/flat-map-ok identity)
                 (result/flat-map-err
                  (constantly {:status 200
                               :body (fs/file (fs/path static-dir "index.html"))})))


             #_(case uri
                 "/js/main.js"
                 {:status 200
                  :headers {"Content-Type" "text/javascript"}
                  :body (fs/file (fs/path static-dir "js/main.js"))}

                 "/css/main.css"
                 {:status 200
                  :headers {"Content-Type" "text/css"}
                  :body (fs/file (fs/path static-dir "css/main.css"))}

                 {:status 200
                  :body (fs/file (fs/path static-dir "index.html"))})

             #_(when-let [f (fs/path static-dir (str/replace-first (URLDecoder/decode uri) #"^/" ""))]
                 (let [index-file (fs/path f "index.html")]
                   #_(index index-file)
                   #_(body index-file)
                   #_(println "URI:" uri index-file)
                   #_{:status 200 :body (fs/file index-file)}

                   (case uri
                     "/js/main.js"
                     {:status 200
                      :headers {"Content-Type" "text/javascript"}
                      :body (fs/file (fs/path static-dir "js/main.js"))}

                     "/css/main.css"
                     {:status 200
                      :headers {"Content-Type" "text/css"}
                      :body (fs/file (fs/path static-dir "css/main.css"))}

                     {:headers {"Content-Type" (ext->mime (fs/file-name f))}
                      :status 200 :body
                      (fs/file (fs/path static-dir "index.html"))})

                   #_(cond
                       (and (fs/directory? f) (fs/readable? index-file))
                       (body index-file)

                       #_#_(fs/directory? f)
                         (index f)

                       (fs/readable? f)
                       (body f)

                       (fs/readable? index-file)
                       {:status 200 :body (fs/file index-file)}

                       :else
                       #_(fs/path "/" (str "index.html" "/" (last (str/split f #"/"))))
                       {:status 404 :body (str "Not found `" f "` in " static-dir)}))))
           {:port port})]
    (println "Starting static server on port:" port)
    (swap! system/system assoc :static-port port :static-server s)))



;; (fs/path "/" (str "index.html" "/" (last (str/split "static/create" #"/"))))
;; (fs/file (fs/path static-dir  (str "index.html" "/center")))
;; (last (str/split "static/create" #"/"))

;; (fs/exists? (fs/path static-dir "js/main.js"))

(defn stop-static-server []
  (when-some [s (:static-server @system/system)]
    (s)
    (swap! system/system assoc :server nil)))
