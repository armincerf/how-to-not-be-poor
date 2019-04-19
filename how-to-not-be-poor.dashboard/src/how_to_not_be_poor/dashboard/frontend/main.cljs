(ns ^:figwheel-hooks how-to-not-be-poor.dashboard.frontend.main
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [how-to-not-be-poor.dashboard.frontend.views :as views]
            [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]))

(defn mount-root []
  (when-let [section (.getElementById js/document "app")]
    (rf/clear-subscription-cache!)
    (r/render [views/main-panel] section)))

(defn init []
  (rf/dispatch-sync [::handlers/initialize-db])
  (enable-console-print!)
  (mount-root))

;; This is called every time you make a code change
(defn ^:after-load reload []
  (mount-root))

(defonce run (init))
