(ns how-to-not-be-poor.dashboard.crux.utils
  (:require [crux.api :as crux]
            [hasch.core :as hasch]
            [clojure.string :as str]))

(defn entity-update
  [system entity-id new-attrs valid-time]
  (let [entity-prev-value (crux/entity (crux/db system) entity-id)]
    (crux/submit-tx system
      [[:crux.tx/put entity-id
        (merge entity-prev-value new-attrs)
        valid-time]])))

(defn q
  ([system query]
   (crux/q (crux/db system) query))
  ([system query valid-time]
   (crux/q (crux/db system valid-time) query)))

(defn entity
  ([system entity-id]
   (crux/entity (crux/db system) entity-id))
  ([system entity-id valid-time]
   (crux/entity (crux/db system valid-time) entity-id)))

(defn entity-with-adjacent
  [system entity-id keys-to-pull]
  (let [db (crux/db system)
        ids->entities
        (fn [ids]
          (cond-> (map #(crux/entity db %) ids)
            (set? ids) set
            (vector? ids) vec))]
    (reduce
      (fn [e adj-k]
        (let [v (get e adj-k)]
          (assoc e adj-k
                 (cond
                   (keyword? v) (crux/entity db v)
                   (or (set? v)
                       (vector? v)) (ids->entities v)
                   :else v))))
      (crux/entity db entity-id)
      keys-to-pull)))

(defn query-pull-first
  ([system query]
   (let [db (crux/db system)]
     (crux/entity
      db (ffirst (crux/q db query)))))
  ([system attribute value]
   (let [db (crux/db system)]
     (crux/entity
      db (ffirst (crux/q db [:find '?e
                             :where ['?e attribute value]]))))))

(defn query-pull-all
  ([system query]
   (let [db (crux/db system)]
     (map
      #(crux/entity
        db (first %))
      (crux/q db query))))
  ([system attribute value]
   (let [db (crux/db system)]
     (map
      #(crux/entity
        db (first %))
      (crux/q db {:find '[?e]
                  :where [['?e attribute value]]})))))

(defn pull-ids
  [system ids]
  (let [db (crux/db system)]
    (map #(crux/entity db %) ids)))

(defn pull-tx
  "pulls only when it sees a given tx time has been transacted"
  [system eid tx-time]
  (crux/entity (crux/db system (java.util.Date.) tx-time) eid))

(defn str->key
  [str]
  (if (str/blank? str)
    (throw (ex-info "String must not be empty" {:str str}))
    (hasch/uuid str)))
