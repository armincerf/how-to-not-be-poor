(ns how-to-not-be-poor.dashboard.frontend.views.info
  (:require [how-to-not-be-poor.dashboard.frontend.common
             :as common :refer [ce rc]]
            [react-admin
             :refer
             [Create
              Datagrid
              DateField
              DisabledInput
              Edit
              EditButton
              Filter
              List
              NumberField
              NumberInput
              ReferenceField
              ReferenceInput
              SelectInput
              SimpleForm
              TextField
              TextInput]]))

(defn name-filter
  [props]
  [:> Filter props
   [:> TextInput {:label "Search" :source "q" :alwaysOn true}]
   [:> ReferenceInput {:label "Name"
                       :source "full_name"
                       :reference "Info"
                       :allowEmpty true}
    [:> SelectInput {:optionText "full_name"}]]])

(defn list-component
  [props]
  [:> List (merge props {:filters (ce (rc name-filter))})
   [:> Datagrid
    [:> TextField {:source "crux.db/id"}]
    [:> TextField {:source "full_name"}]
    [:> TextField {:source "addresses"}]
    [:> TextField {:source "emails"}]
    [:> TextField {:source "phones"}]
    [:> DateField {:source "update_timestamp"}]]])
