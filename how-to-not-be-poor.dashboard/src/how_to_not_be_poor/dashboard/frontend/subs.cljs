(ns how-to-not-be-poor.dashboard.frontend.subs
    (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::greetings
 (fn [db _]
   (:greetings db)))

(reg-sub
 ::greeting-index
 (fn [db _]
   (:greeting-index db)))

(reg-sub
 ::store
 (fn [db _]
   (:store db)))

(reg-sub
 ::table-details
 (fn [db _]
   (:table-details db)))
