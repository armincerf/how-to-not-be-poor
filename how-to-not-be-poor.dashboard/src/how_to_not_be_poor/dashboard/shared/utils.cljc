(ns how-to-not-be-poor.dashboard.shared.utils
  #?(:cljs
     (:import
      (goog.i18n NumberFormat))))

#?(:cljs
   (set! goog.i18n.NumberFormatSymbols
         goog.i18n.NumberFormatSymbols_en_GB))

(defn sum-key
  [key-fn coll]
  (if (seq coll)
    (transduce (comp (remove #(nil? (key-fn %)))
                     (map key-fn)) #?(:clj +' :cljs +) 0 coll)
    0))

(def ^:private money-formatter
  #?(:cljs
     (goog.i18n.NumberFormat.
      (.-CURRENCY goog.i18n.NumberFormat.Format))
     :clj
     (java.text.NumberFormat/getCurrencyInstance (new java.util.Locale "en" "GB"))))

(defn format-amount
  [n]
  (when n
    [:div
     {:style {:color (if (neg? n) :red :green)}}
     (.format money-formatter n)]))
