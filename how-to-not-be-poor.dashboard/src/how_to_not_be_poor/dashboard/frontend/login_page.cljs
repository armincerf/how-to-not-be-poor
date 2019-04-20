(ns how-to-not-be-poor.dashboard.frontend.login-page
  (:require [react-admin :refer [userLogin]]
            [react-redux :refer [connect]]
            [how-to-not-be-poor.dashboard.frontend.common :refer [rc]]
            [material-ui-core :refer
             [withStyles Icon MuiThemeProvider createMuiTheme Login]]))

(defn login-form
  []
  (let [callback "http://localhost:7979/callback"]
    [:a {:href (str "https://auth.truelayer.com/?response_type=code&client_id=amipoor-944004&nonce=" (gensym) "&scope=info%20accounts%20balance%20cards%20transactions%20direct_debits%20standing_orders%20products%20beneficiaries%20offline_access&redirect_uri="
                    callback
                    "&enable_mock=true&enable_oauth_providers=true&enable_open_banking_providers=true&enable_credentials_sharing_providers=true")}
     "Click here to log in"]))


