(ns app.common.validator.password
  (:require
   [app.common.config :as config]
   [app.common.validator.validator :refer [pipe]]))

(defn password-input [p]
  (pipe (vals (config/validators :password-input)) p))
