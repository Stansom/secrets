(ns app.common.result)

(defn ok [v]
  {:ok v})

(defn err [v]
  {:err v})

(defn ok? [r]
  (when (map? r)
    (contains? r :ok)))

(defn err? [r]
  (when (map? r)
    (contains? r :err)))

(defn map-ok [r f]
  (if (ok? r)
    {:ok (f (:ok r))}
    r))

(defn flat-map-ok [r f]
  (if (ok? r)
    (f (:ok r))
    r))

(defn map-err [r f]
  (if (err? r)
    {:err (f (:err r))}
    r))

(defn flat-map-err [r f]
  (if (err? r)
    (f (:err r))
    r))

(defn of [v pred]
  (if (pred v)
    (ok v)
    (err v)))

(defn match [r on-ok on-err]
  (cond 
    (:ok r) (-> r :ok on-ok )
    (:err r) (-> r :err on-err)
    :else "can't match result"
    )
  )

(comment
  (of (get {:x 5} :a) #(and (not (nil? %)) (> % 10))))

