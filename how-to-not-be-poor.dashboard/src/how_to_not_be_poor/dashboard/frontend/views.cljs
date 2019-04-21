(ns how-to-not-be-poor.dashboard.frontend.views
    (:require [reagent.core :as r :refer [atom]]
              [cljs.pprint :as pp :refer [pprint]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [how-to-not-be-poor.dashboard.frontend.views.transactions :as transactions]
              [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
              [how-to-not-be-poor.dashboard.frontend.views.info :as info]
              [how-to-not-be-poor.dashboard.frontend.subs :as subs]
              [how-to-not-be-poor.dashboard.frontend.views.cards :as cards]
              [how-to-not-be-poor.dashboard.frontend.auth-provider :as auth]
              [how-to-not-be-poor.dashboard.frontend.login-page :as login-page]
              [how-to-not-be-poor.dashboard.frontend.common :refer [rc ce]]
              [material-ui-core :refer [Icon Card CardContent CardHeader]]
              [data-provider]
              [react-admin :refer [Admin ListGuesser Resource Login]]))

(def oauth-url (str "https://auth.truelayer.com/?response_type=code&client_id=amipoor-944004&nonce="
                    (gensym)
                    "&scope=info%20accounts%20balance%20cards%20transactions%20direct_debits%20standing_orders%20products%20beneficiaries%20offline_access&redirect_uri="
                    "http://localhost:7979/callback"
                    "&enable_mock=true&enable_oauth_providers=true&enable_open_banking_providers=true&enable_credentials_sharing_providers=true"))

(defn pprint-code
  [obj]
  [:code [:pre (with-out-str (pprint obj))]])

(defn dashboard
  []
  (let [store (subscribe [::subs/store])]
    [:> Card
     [:> CardHeader {:title "Welcome!"}]
     [:> CardContent (pprint-code @store)]
     [:> CardContent
      [:button {:on-click #(dispatch [::handlers/get-store])} "Load data"]]
     [:> CardContent
      [:a {:href oauth-url}
       "Click here to add a bank account"]]]))

(defn main-panel
  []
  [:> Admin {:dataProvider (data-provider "/admin-api")
             :dashboard (rc dashboard)}
   [:> Resource {:name "Info"
                 :list (rc info/list-component)}]
   [:> Resource {:name "Cards"
                 :list (rc cards/list-component)}]
   [:> Resource {:name "Transactions"
                 :list (rc transactions/list-component)}]])
