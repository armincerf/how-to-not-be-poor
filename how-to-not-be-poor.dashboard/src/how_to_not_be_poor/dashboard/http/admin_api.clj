(ns how-to-not-be-poor.dashboard.http.admin-api
  (:require [clojure.string :as str]
            [clojure.tools.logging :as logging]
            [how-to-not-be-poor.dashboard.http.auth :refer [store]]
            [integrant.core :as ig]
            [medley.core :as medley]
            [yada.yada :as yada]))

(defn- parse-route-params
  [{:keys [table-name-string id] :as params}]
  {:table (keyword table-name-string)
   :id (when id
         (try (Integer/parseInt id)
              (catch NumberFormatException e
                (logging/warn "can't parse id" e params)
                nil)))})

(defn get-item
  [ctx]
  (let [{:keys [table id]} (parse-route-params (:route-params ctx))]
    (get-in @store [:data table id])))

(defn put-item
  [ctx]
  (let [{:keys [table id]} (parse-route-params (:route-params ctx))]
    (swap! store #(assoc-in % [:data table id] (:body ctx)))
    (get-item ctx)))

(defn create-item
  [ctx]
  (let [table (keyword (:table-name-string (:route-params ctx)))]
    (swap! store #(assoc-in % [:data table (:id (:body ctx))] (:value (:body ctx))))
    (get-in @store [:data table (:id (:body ctx))])))

(defn delete-item
  [ctx]
  (let [{:keys [table id]} (parse-route-params (:route-params ctx))]
    nil))

(defn- parse-string-query-param
  [key query-params]
  (some->> key
           (get query-params)
           .toLowerCase
           keyword))

(defn- parse-int-query-param
  [key query-params]
  (some->> key
           (get query-params)
           Integer/parseInt))

(defn- count-rows
  [table where-clause]
  (when table
    10))

(defn- build-where-clause
  [ids]
  1)

;; very naive for now, ignores pagination and referenced attributes. should
;; replace with fulltext search when appropriate
(defn- filter-results-by-search
  [results search-text]
  (filter (fn [result]
            (some (fn [[_ v]]
                    (.contains (.toLowerCase (str v))
                               (.toLowerCase search-text))) result))
          results))

(defn get-many
  [{:keys [parameters response]}]
  (let [{:keys [table-name-string]} (:path parameters)
        table (.toLowerCase table-name-string)
        query-params (:query parameters)

        order (parse-string-query-param "_order" query-params)
        sort (parse-string-query-param "_sort" query-params)
        start (parse-int-query-param "_start" query-params)
        end (parse-int-query-param "_end" query-params)

        ;; query params without a starting '_' except q are id filters
        id-filters (some->> query-params
                         (medley/remove-keys #(or (str/starts-with? % "_")
                                                  (= % "q"))))

        search-text (get query-params "q")

        where-clause (build-where-clause id-filters)
        query-result (map #(assoc % :id (hash (or (:account_id %) (first %)))) (get-in @store [:data table]))
        result (if search-text
                 (filter-results-by-search query-result search-text)
                 query-result)
        row-count (if search-text
                    (count result)
                    (count-rows table where-clause))]
    (merge response
           {:headers {"x-total-count" row-count}
            :body (if (seq id-filters) (take 1 result) result)})))

(defmethod ig/init-key ::admin-handler
  [id _]
  [[[:table-name-string "/" :id]
    (yada/resource
     {:id id
      :methods
      {:get
       {:produces {:media-type "application/json"}
        :response #(get-item %)}
       :put
       {:produces {:media-type "application/json"}
        :consumes {:media-type "application/json"}
        :response #(put-item %)}
       :delete
       {:produces {:media-type "application/json"}
        :response #(delete-item %)}}})]
   [[:table-name-string]
    (yada/resource
     {:id id
      :methods
      {:get
       {:produces {:media-type "application/json"}
        :response #(get-many %)}
       :post
       {:produces {:media-type "application/json"}
        :consumes {:media-type "application/json"}
        :response #(create-item %)}}})]])