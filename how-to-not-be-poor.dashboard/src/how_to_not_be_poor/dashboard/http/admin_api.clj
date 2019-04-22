(ns how-to-not-be-poor.dashboard.http.admin-api
  (:require [clojure.string :as str]
            [clojure.tools.logging :as logging]
            [edge.sse.event-stream :as sse]
            [how-to-not-be-poor.dashboard.crux.utils :as crux.utils]
            [how-to-not-be-poor.dashboard.shared.utils :as utils]
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
    ;(get-in @store [:data table id])
    ))

(defn put-item
  [ctx]
  (let [{:keys [table id]} (parse-route-params (:route-params ctx))]
    ;(swap! store #(assoc-in % [:data table id] (:body ctx)))
    (get-item ctx)))

(defn create-item
  [ctx]
  (let [table (keyword (:table-name-string (:route-params ctx)))]
    ;(swap! store #(assoc-in % [:data table (:id (:body ctx))] (:value (:body ctx))))
    ;(get-in @store [:data table (:id (:body ctx))])
    ))

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

(defn- crux-get-many
  [system table id-filters]
  (let [ids (vals id-filters)]
    (map #(assoc % :id (:crux.db/id %))
         (if ids
           (crux.utils/pull-ids system ids)
           (crux.utils/query-pull-all system :table-name table)))))

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
  [system bus {:keys [parameters response]}]
  (let [{:keys [table-name-string]} (:path parameters)
        table (keyword (.toLowerCase table-name-string))
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
        result (crux-get-many system table id-filters)
        result (if search-text
                 (filter-results-by-search result search-text)
                 result)
        _ (def result result)
        row-count (count result)
        sorted-result
        (cond->> result
          (and (some? order)
               (some? sort))
          (sort-by (keyword sort)
                   (if (= :desc order)
                     (comp - compare)
                     compare))
          (int? end)
          ((fn [s e r]
             (into [] (subvec (vec r) s e))) start (min end row-count)))]
    (when (= :transactions table)
      (sse/publish-global-event
       bus {:event :sse-store
            :body {:key :table-details
                   :value {:sum (reduce (fn sum-transactions
                                          [n tx]
                                          (if (= "CREDIT" (:transaction_type tx))
                                            (+ n (Math/abs (:amount tx)))
                                            (- n (Math/abs (:amount tx)))))
                                        0 result)}}}))
    (merge response
           {:headers {"x-total-count" row-count}
            :body  (if (zero? row-count)
                     []
                     sorted-result)})))

(defmethod ig/init-key ::admin-handler
  [id {:keys [system event-bus]}]
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
        :response #(get-many system event-bus %)}
       :post
       {:produces {:media-type "application/json"}
        :consumes {:media-type "application/json"}
        :response #(create-item %)}}})]])
