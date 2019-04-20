(ns how-to-not-be-poor.dashboard.frontend.auth-provider
  (:require [react-admin :refer [AUTH_LOGIN AUTH_LOGOUT AUTH_ERROR AUTH_CHECK]]
            [how-to-not-be-poor.dashboard.frontend.handlers :as handlers]
            [re-frame.core :as rf]))

(defn auth-provider
  ;; TODO link to core API and add roles to main db for users, read only admin
  ;; and read/write admin (god mode) and figure out react-admin authorization
  [type params]
  (js/console.log params)
  (let [username (when params (.-username params))
        password (when params (.-password params))]
    (condp = type
      AUTH_LOGIN (do (js/localStorage.setItem "username" username)
                     (rf/dispatch [::handlers/login
                                   {:username username :password password}])
                     (js/Promise.resolve))
      AUTH_ERROR (do (js/localStorage.removeItem "username")
                     (js/Promise.resolve))
      AUTH_LOGOUT (do (js/localStorage.removeItem "username")
                      (js/Promise.resolve))
      AUTH_CHECK (if (js/localStorage.getItem "username")
                   (js/Promise.resolve)
                   (js/Promise.reject))
      (js/Promise.reject "unknown method"))))
