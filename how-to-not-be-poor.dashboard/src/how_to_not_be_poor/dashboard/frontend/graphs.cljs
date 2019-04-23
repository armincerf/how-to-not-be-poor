(ns how-to-not-be-poor.dashboard.frontend.graphs
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [cljsjs.vega]
            [cljsjs.vega-lite]
            [cljsjs.vega-embed]
            [cljsjs.vega-tooltip]))

(defn- ^:no-doc log [a-thing]
  (.log js/console a-thing))


(defn ^:no-doc render-vega-lite
  ([spec elem]
   (when spec
     (let [spec (clj->js spec)
           opts {:renderer "canvas"
                 :mode "vega-lite"}
           vega-spec (. js/vl (compile spec))]
       (log "Vega-lite translates to:")
       (log vega-spec)
       (-> (js/vegaEmbed elem spec (clj->js opts))
           (.then (fn [res]
                    #_(log res)
                    (. js/vegaTooltip (vegaLite (.-view res) spec))))
           (.catch (fn [err]
                     (log err))))))))

(defn render-vega [spec elem]
  (when spec
    (let [spec (clj->js spec)
          opts {:renderer "canvas"
                :mode "vega"}]
      (-> (js/vegaEmbed elem spec (clj->js opts))
          (.then (fn [res]
                   #_(log res)
                   (. js/vegaTooltip (vega (.-view res) spec))))
          (.catch (fn [err]
                    (log err)))))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  [spec]
  (r/create-class
   {:display-name "vega-lite"
    :component-did-mount (fn [this]
                           (render-vega-lite spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (render-vega-lite new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))


(defn vega
  "Reagent component that renders vega"
  [spec]
  (r/create-class
   {:display-name "vega"
    :component-did-mount (fn [this]
                           (render-vega spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (render-vega new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))

(defn amount-over-time
  [min max]
  {:data
   {:url "/store"
    :format
    {:type "json" :parse {:timestamp "date"}}}
   :transform [{:window [{:op "row_number" :as "row_number"}]}
               {:filter (str "datum.amount < " max)}
               {:filter (str "datum.amount > " min)}]
   :hconcat
   [{:selection {:brush {:type "interval"}}
     :mark "point"
     :encoding
     {:x
      {:field "timestamp"
       :type "temporal"
       :axis {:title "Time"}}
      :y {:field "amount"
          :type "quantitative"
          :axis {:title "Amount"}}}}
    {:transform
     [{:filter {:selection "brush"}}
      {:window [{:op "rank" :as "rank"}]}
      {:filter {:field "rank" :lt 20}}]
     :hconcat
     [{:title "Account name"
       :mark "text"
       :width 200
       :encoding
       {:text
        {:field "display_name"
         :type "nominal"}
        :y
        {:field "row_number"
         :type "ordinal"
         :axis nil}}}
      {:title "Descripion"
       :mark "text"
       :width 300
       :encoding
       {:text {:field "description" :type "nominal"}
        :y
        {:field "row_number"
         :type "ordinal"
         :axis nil}}}
      {:title "Amount"
       :mark "text"
       :encoding
       {:text
        {:field "amount"
         :type "nominal"}
        :y
        {:field "row_number"
         :type "ordinal"
         :axis nil}}}]}]
   :resolve {:legend {:color "independent"}}})
