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

(def amount-over-time
  {:data
   {:url "/store",
    :format
    {:type "json", :parse {:timestamp "date"}}},
   :vconcat
   [{:width 480,
     :mark "area",
     :encoding
     {:x
      {:field "timestamp",
       :type "temporal",
       :scale {:domain {:selection "brush"}},
       :axis {:title ""}},
      :y {:field "amount", :type "quantitative"}}}
    {:width 480,
     :height 60,
     :mark "area",
     :selection
     {:brush
      {:type "interval", :encodings ["x"]}},
     :encoding
     {:x {:field "timestamp", :type "temporal"},
      :y
      {:field "amount",
       :type "quantitative",
       :axis {:tickCount 3, :grid false}}}}]})
