(ns how-to-not-be-poor.dashboard.frontend.handlers
  (:require
    [re-frame.core :as rf]
    [com.yetanalytics.sse-fx.event-source :as event-source]
    [com.yetanalytics.sse-fx.events :refer [register-all!]]
    [how-to-not-be-poor.dashboard.frontend.ajax :as ajax]
    [day8.re-frame.http-fx]
    [how-to-not-be-poor.dashboard.frontend.db :as db]))

(register-all!)

;; -- Handlers --------------------------------------------------------------

(rf/reg-event-fx
  ::initialize-db
  (fn [{:keys [db]} _]
    {:db db/app-db
     :dispatch [::init-event-source]}))

(rf/reg-event-fx
 ::init-event-source
 (fn [{:keys [db] :as ctx} _]
   {::event-source/init
    {:uri "/events"
     :handle-message [::sse-handle-message]}}))

(rf/reg-event-fx
 ::sse-handle-message
 (fn [_ [_ _ e]]
   {:dispatch [(:event e) (:body e)]}))

(rf/reg-event-db
 :sse-store
 (fn [db [_ response]]
   (assoc db (:key response) (:value response))))

(rf/reg-event-db
 :update-progress
 (fn [db [_ response]]
   (update db (:key response)
           #(merge % (:value response)))))

(rf/reg-event-fx
 ::generic-ajax-failure
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

(rf/reg-event-fx
 ::get-store
 (fn [{db :db} [_ params]]
   (ajax/get-request "/store"
                      [::store-success]
                      [::generic-ajax-failure])))

(rf/reg-event-db
 ::store-success
 (fn [db [_ response]]
   (assoc db :store response)))

