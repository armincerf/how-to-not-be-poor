(ns how-to-not-be-poor.dashboard.frontend.handlers
  (:require
    [re-frame.core :as rf]
    [how-to-not-be-poor.dashboard.frontend.ajax :as ajax]
    [day8.re-frame.http-fx]
    [how-to-not-be-poor.dashboard.frontend.db :as db]))

;; -- Handlers --------------------------------------------------------------

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/app-db))

(rf/reg-event-fx
 :generic-ajax-failure
 (fn [{db :db} [_ response]]
   (js/console.error "bad response " response)))

(rf/reg-event-fx
 ::truelayer-creds
 (fn [{db :db} [_ result]]
   (ajax/get-request "/creds"
                     [:truelayer-creds-success]
                     [:generic-ajax-failure])))

(rf/reg-event-fx
 ::login
 (fn [{db :db} [_ params]]
   (ajax/post-request "/login"
                      params
                      [:login-success]
                      [:generic-ajax-failure])))
