(ns how-to-not-be-poor.dashboard.frontend.common
  (:require [reagent.core :as reagent]
            [react-admin :refer [Filter TextInput]]))

(defn search-filter
  [props]
  [:> Filter props
   [:> TextInput {:label "Search" :source "q" :alwaysOn true}]])

(def rc reagent/reactify-component)
(def ce reagent/create-element)

