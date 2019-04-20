(ns how-to-not-be-poor.dashboard.frontend.views
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
              [how-to-not-be-poor.dashboard.frontend.subs :as subs]
              [how-to-not-be-poor.dashboard.frontend.auth-provider :as auth]
              [how-to-not-be-poor.dashboard.frontend.login-page :as login-page]
              [how-to-not-be-poor.dashboard.frontend.common :refer [rc ce]]
              [material-ui-core :refer [Icon]]
              [data-provider]
              [react-admin :refer [Admin ListGuesser Resource Login]]))

(defn main-panel
  []
  [:> Admin {:dataProvider (data-provider "/admin")
             :loginPage (rc (fn [] [:> Login {:loginForm (ce (rc login-page/login-form))}]))
             :authProvider auth/auth-provider}
   [:> Resource {:name "foo"}]])
