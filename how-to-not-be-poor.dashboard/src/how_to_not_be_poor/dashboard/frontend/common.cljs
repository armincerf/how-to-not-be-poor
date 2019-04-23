(ns how-to-not-be-poor.dashboard.frontend.common
  (:require [reagent.core :as reagent]
            [cljs.pprint :as pp :refer [pprint]]
            [react-admin :refer [Filter TextInput]])
  (:import [goog.async Debouncer]))

(defn search-filter
  [props]
  [:> Filter props
   [:> TextInput {:label "Search" :source "q" :alwaysOn true}]])

(defn pprint-code
  [obj]
  [:code [:pre (with-out-str (pprint obj))]])

(def rc reagent/reactify-component)
(def ce reagent/create-element)

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))
