(ns how-to-not-be-poor.dashboard.frontend.views
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
              [how-to-not-be-poor.dashboard.frontend.subs :as subs]))

(defn main-panel
  []
  (let [greetings (subscribe [::subs/greetings])
        greeting-index (subscribe [::subs/greeting-index])]
    (fn []
      [:div
       [:p (get @greetings @greeting-index)]
       [:button
        {:on-click #(dispatch [::handlers/set-random-greeting-index])}
        [:p "press me!"]]])))
