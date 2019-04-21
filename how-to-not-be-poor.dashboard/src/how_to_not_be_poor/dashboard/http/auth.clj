(ns how-to-not-be-poor.dashboard.http.auth
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [crux.api :as crux]
   [how-to-not-be-poor.dashboard.crux.utils :as crux.utils]
   [integrant.core :as ig]
   [schema.core :as s]
   [clj-http.client :as http]
   [yada.yada :as yada]))

(def creds (edn/read-string (slurp (io/resource "credentials.edn"))))

(def base-uri "https://api.truelayer.com/data/v1/")

(defn- get-tokens
  [system]
  (crux.utils/entity system :truelayer-tokens))

(defn- store-tokens
  [system access refresh]
  (prn "storing new tokens, old = " (:refresh-token (get-tokens system)) "new = " refresh)
  (crux.api/submit-tx
   system
   [[:crux.tx/put :truelayer-tokens
     {:crux.db/id :truelayer-tokens
      :access-token access
      :refresh-token refresh}]]))

(defn renew-token!
  [system refresh-token]
  (let [{:keys [access_token refresh_token] :as response}
        (http/post "https://auth.truelayer.com/connect/token"
                   {:form-params
                    (merge creds
                           {:grant_type "refresh_token"
                            :refresh_token refresh-token})
                    :as :json})]
    (prn "refreshing token" refresh-token)
    (when access_token
      (store-tokens system access_token refresh_token))
    (some? access_token)))

(defn transact-response
  [system response table-name id-key]
  (prn "transacting") 
  (prn "transacted"
       (crux.api/submit-tx
        system
        (vec (for [result (-> response
                              :body
                              :results)
                   :let [id (crux.utils/str->key
                             (id-key result))]]
               [:crux.tx/put id
                (merge
                 {:crux.db/id id
                  :table-name (keyword table-name)}
                 result)
                (clojure.instant/read-instant-date
                 (or (:update_timestamp result)
                     (:timestamp result)))])))))

(defn get-data
  ([system table-name id-key]
   (get-data system table-name id-key (str base-uri table-name)))
  ([system table-name id-key uri]
   (let [{:keys [access-token refresh-token]} (get-tokens system)]
     (http/get uri
               {:headers {"Authorization" (str "Bearer " access-token)}
                :as :json
                :async? true}
               ;; respond callback
               (fn [response]
                 (transact-response system response table-name id-key)
                 (cond
                   (= table-name "accounts")
                   (doseq [result (-> response :body :results)]
                     (get-data
                      system
                      "transactions"
                      :transaction_id
                      (str base-uri "accounts/" (id-key result) "/transactions")))
                   (= table-name "cards")
                   (doseq [result (-> response :body :results)]
                     (get-data
                      system
                      "transactions"
                      :transaction_id
                      (str base-uri "cards/" (id-key result) "/transactions")))))
               ;; raise callback
               (fn [exception]
                 (println
                  table-name
                  "exception message is: "
                  (.getMessage exception)))))))

(defn download-data
  [system]
  (get-data system "info" :full_name)
  (get-data system "accounts" :account_id)
  (get-data system "cards" :account_id))

(defn authenticate-truelayer
  [ctx system]
  (let [{:keys [access_token refresh_token] :as response}
        (:body (http/post
                "https://auth.truelayer.com/connect/token"
                {:form-params
                 (merge
                  creds
                  {:grant_type "authorization_code"
                   :redirect_uri "http://localhost:7979/callback"
                   :code (get-in ctx [:parameters :query "code"])})
                 :as :json}))
        {:keys [crux.tx/tx-time]}
        (store-tokens system access_token refresh_token)
        {:keys [access-token]}
        (crux.utils/pull-tx system :truelayer-tokens tx-time)]
    (when (some? access-token)
      (download-data system))
    (some? access-token)))

(defmethod ig/init-key ::login
  [id {:keys [system]}]
  (yada/resource
   {:id id
    :methods
    {:post
     {:consumes {:media-type "application/json"}
      :produces {:media-type "application/json"}
      :response (fn [ctx]
                  (let [user (authenticate-truelayer ctx system)]
                    (cond
                      (= "suspended" (:status user))
                      (merge (:response ctx)
                             {:status 403
                              :body {:key :username
                                     :msg "Your account is suspended"}})
                      (nil? user)
                      (merge (:response ctx)
                             {:status 401
                              :body {:key :username
                                     :msg "User not found"}})
                      :else user)))}}}))

(defmethod ig/init-key ::callback
  [id {:keys [system]}]
  (yada/resource
   {:id id
    :methods
    {:get
     {:produces {:media-type "application/json"}
      :response (fn [ctx]
                  (let [user (authenticate-truelayer ctx system)]
                    (cond
                      (= "suspended" (:status user))
                      (merge (:response ctx)
                             {:status 403
                              :body {:key :username
                                     :msg "Your account is suspended"}})
                      (nil? user)
                      (merge (:response ctx)
                             {:status 401
                              :body {:key :username
                                     :msg "User not found"}})
                      :else
                      (merge
                       (:response ctx)
                       {:status 303
                        :headers {"Location" "/"}}))))}}}))

(defmethod ig/init-key ::store
  [id {:keys [system]}]
  (yada/resource
   {:id id
    :methods
    {:get
     {:produces {:media-type "application/json"}
      :response (fn [ctx]
                  (def system system)
                  (def db (crux/db system))
                  (let [db (crux/db system)
                        data (crux.utils/query-pull-all system :table-name :info)]
                    (or data
                        {:error "No data yet, please add an account"})))}}}))
