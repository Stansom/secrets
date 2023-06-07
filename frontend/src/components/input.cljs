(ns components.input)

(defn input [attrs value]
  [:input (assoc attrs :on-change #(reset! value (-> % .-target .-value)))])