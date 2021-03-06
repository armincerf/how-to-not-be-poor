(ns how-to-not-be-poor.dashboard.frontend.views.cards
  (:require [react-admin
             :refer
             [Create
              DateField
              Datagrid
              DisabledInput
              Edit
              EditButton
              List
              NumberField
              NumberInput
              ReferenceField
              ReferenceInput
              SelectInput
              SimpleForm
              TextField
              TextInput]]))

(defn list-component
  [props]
  [:> List props
   [:> Datagrid
    [:> TextField {:source "crux.db/id"}]
    [:> TextField {:source "partial_card_number"}]
    [:> TextField {:source "name_on_card"}]
    [:> TextField {:source "currency"}]
    [:> TextField {:source "card_network"}]
    [:> TextField {:source "display_name"}]
    [:> TextField {:source "provider.display_name"}]
    [:> TextField {:source "card_type"}]
    [:> DateField {:source "update_timestamp"}]]])

