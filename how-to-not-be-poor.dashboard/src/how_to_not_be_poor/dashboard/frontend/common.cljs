(ns how-to-not-be-poor.dashboard.frontend.common
  (:require [reagent.core :as reagent]
            [cljs.pprint :as pp :refer [pprint]]
            [react-admin :refer [Filter TextInput]]))

(defn search-filter
  [props]
  [:> Filter props
   [:> TextInput {:label "Search" :source "q" :alwaysOn true}]])

(defn pprint-code
  [obj]
  [:code [:pre (with-out-str (pprint obj))]])

(def rc reagent/reactify-component)
(def ce reagent/create-element)

