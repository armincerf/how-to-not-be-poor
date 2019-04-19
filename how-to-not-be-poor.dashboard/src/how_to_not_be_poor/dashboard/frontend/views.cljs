(ns how-to-not-be-poor.dashboard.frontend.views
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
              [how-to-not-be-poor.dashboard.frontend.subs :as subs]
              [material-ui-core :refer [Icon]]
              [data-provider]
              [react-admin :refer [Admin ListGuesser Resource]]))

(defn main-panel
  []
  [:> Admin {:dataProvider (data-provider "/admin")}
   [:> Resource {:name "foo"}]])
