(ns how-to-not-be-poor.dashboard.frontend.views
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [how-to-not-be-poor.dashboard.frontend.views.transactions :as transactions]
              [ra-language-english :as ra-english]
              [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
              [how-to-not-be-poor.dashboard.frontend.graphs :as graphs]
              [how-to-not-be-poor.dashboard.frontend.views.info :as info]
              [how-to-not-be-poor.dashboard.frontend.subs :as subs]
              [how-to-not-be-poor.dashboard.frontend.views.cards :as cards]
              [how-to-not-be-poor.dashboard.frontend.auth-provider :as auth]
              [how-to-not-be-poor.dashboard.frontend.login-page :as login-page]
              [how-to-not-be-poor.dashboard.frontend.common :refer [rc ce] :as common]
              [material-ui-core :refer [Icon Card CardContent CardHeader withStyles
                                        CircularProgress ListItem ListItemText
                                        ListItemSecondaryAction ListItemIcon List]]
              [data-provider]
              [react-admin :refer [Admin ListGuesser Resource Login mergeTranslations] :as admin])
    (:import [goog.async Debouncer]))

(def oauth-url (str "https://auth.truelayer.com/?response_type=code&client_id=amipoor-944004&nonce="
                    (gensym)
                    "&scope=info%20accounts%20balance%20cards%20transactions%20direct_debits%20standing_orders%20products%20beneficiaries%20offline_access&redirect_uri="
                    "http://localhost:7979/callback"
                    "&enable_mock=true&enable_oauth_providers=true&enable_open_banking_providers=true&enable_credentials_sharing_providers=true"))

(defn material-ui-icon
  [icon-name]
  (fn []
    [:> Icon icon-name]))

(def account-button
  ((withStyles #js {:button #js {:marginTop "1em"}})
   (rc
    (fn [{:keys [classes record]}]
      [:> admin/Button
       {:className (.-button classes)
        :variant "raised"
        :href oauth-url
        :label "Add New Account"
        :title "Add New Account"}
       [:> Icon "playlist_add"]]))))

(defn dashboard
  []
  (let [slider-max (atom 20)
        slider-min (atom 0)]
    (fn []
      (let [progress (subscribe [::subs/account-progress])
            update-slider-max (common/debounce #(reset! slider-max %) 100)
            update-slider-min (common/debounce #(reset! slider-min %) 100)]
        [:<>
         [:> Card
          [:> CardHeader {:title "Welcome!"}]
          [:> CardContent
           [:div {:style {:width "220px"}}
            [:> List
             (when (seq @progress)
               (for [[k v] @progress]
                 ^{:key k}
                 [:> ListItem
                  [:> ListItemText {:primary (str k)}]
                  [:> ListItemSecondaryAction
                   (cond
                     (or (= "loading" v)
                         (.includes v "pending"))
                     [:> CircularProgress {:size 25}]
                     :else
                     [:> Icon "check_circle"])]]))]]]
          (when (seq @progress)
            [:<>
             [:> CardContent "Your spending"]
             [:> CardContent
              [:div.slidercontainer
               [:p "Max amount = " @slider-max]
               [:input {:type "range"
                        :min "-1000"
                        :max "1000"
                        :class "slider"
                        :on-change
                        #(update-slider-max (-> % .-target .-value))}]]]
             [:> CardContent
              [:div.slidercontainer
               [:p "Min amount = " @slider-min]
               [:input {:type "range"
                        :min "-1000"
                        :max "1000"
                        :class "slider"
                        :on-change
                        #(update-slider-min (-> % .-target .-value))}]]]
             [:> CardContent
              [graphs/vega-lite (graphs/amount-over-time @slider-min @slider-max)]]])
          [:> CardContent
           [:> account-button]]]]))))

(defn main-panel
  []
  [:> Admin {:dataProvider (data-provider "/admin-api")
             :dashboard (rc dashboard)}
   [:> Resource {:name "Info"
                 :list (rc info/list-component)}]
   [:> Resource {:name "Accounts"
                 :list (rc cards/list-component)}]
   [:> Resource {:name "Cards"
                 :list (rc cards/list-component)}]
   [:> Resource {:name "Transactions"
                 :list (rc transactions/list-component)}]
   [:> Resource {:name "Tags"}]])
