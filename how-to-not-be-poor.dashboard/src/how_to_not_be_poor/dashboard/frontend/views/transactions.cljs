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
              Filter
              ReferenceField
              ReferenceInput
              SelectInput
              SimpleForm
              TextField
              TextInput]]
            [material-ui-core :refer [Typography]]
            [how-to-not-be-poor.dashboard.frontend.common
             :as common :refer [ce rc]]
            [how-to-not-be-poor.dashboard.frontend.subs :as sub]
            [how-to-not-be-poor.dashboard.shared.utils :as utils]
            [re-frame.core :as rf]))

(defn list-component
  [props]
  (let [{:keys [sum] :as details} @(rf/subscribe [::sub/table-details])]
    [:<>
     [:> List (merge props {:filters (ce (rc common/search-filter))})
      [:> Datagrid
       [:> TextField {:source "description"}]
       [:> NumberField {:source "amount"}]
       [:> TextField {:source "transaction_classification"}]
       [:> TextField {:source "merchant_name"}]
       [:> TextField {:source "display_name"}]
       [:> TextField {:source "transaction_type"}]
       [:> DateField {:source "timestamp"}]]]
     [:div {:style {:width 300 :margin "1em"}}
      [:> Typography {:variant "title"} "Total: " (utils/format-amount sum)]]]))
