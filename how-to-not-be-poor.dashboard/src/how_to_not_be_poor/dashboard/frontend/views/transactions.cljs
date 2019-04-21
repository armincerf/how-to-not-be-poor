(ns how-to-not-be-poor.dashboard.frontend.views.transactions
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
    [:> TextField {:source "description"}]
    [:> NumberField {:source "amount"}]
    [:> TextField {:source "transaction_classification"}]
    [:> TextField {:source "meta.provider_transaction_category"}]
    [:> TextField {:source "transaction_category"}]
    [:> TextField {:source "transaction_type"}]
    [:> TextField {:source "currency"}]
    [:> TextField {:source "merchant_name"}]
    [:> DateField {:source "timestamp"}]]])
