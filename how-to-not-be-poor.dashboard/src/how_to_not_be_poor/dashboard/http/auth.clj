(ns how-to-not-be-poor.dashboard.http.auth
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [tick.alpha.api :as tick]
   [edge.sse.event-stream :as sse]
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
  [system results table-name id-key]
  (prn "transacting")
  (prn "transacted"
       (crux.api/submit-tx
        system
        (vec (for [result results
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

(defn update-progress
  [bus table-name result]
  (sse/publish-global-event
   bus {:event :update-progress
        :body {:key :account-progress
               :value (merge {(keyword (str "fetch-" table-name)) result}
                             (when-not (= "transactions" table-name)
                               {:fetch-transactions "loading"}))}}))

(defn get-data
  [system bus {:keys [table-name id-key uri display-name]
               :or {uri (str base-uri table-name)}}]
  (let [{:keys [access-token refresh-token]} (get-tokens system)]
    (http/get uri
              {:headers {"Authorization" (str "Bearer " access-token)}
               :as :json
               :async? true}
              ;; respond callback
              (fn [response]
                (let [results (if (= "transactions" table-name)
                                 (map (fn [tx]
                                        (assoc tx :display_name display-name))
                                      (-> response
                                          :body
                                          :results))
                                 (-> response :body :results))]
                  (transact-response system results table-name id-key)
                  (update-progress bus table-name "success")
                  (let [fetch-transactions
                        (fn [from result]
                          (let [account-id (id-key result)]
                            (prn "keys" (keys result) (:display_name result))
                            (get-data
                             system
                             bus
                             {:table-name "transactions"
                              :id-key :transaction_id
                              :display-name (:display_name result)
                              :uri (str base-uri from "/"
                                        account-id
                                        "/transactions?from="
                                        "2018-05-01"
                                        "&to="
                                        (tick/today))})))]
                    (cond
                      (= table-name "accounts")
                      (doseq [result results]
                        (fetch-transactions table-name result))
                      (= table-name "cards")
                      (doseq [result results]
                        (fetch-transactions table-name result))))))
              ;; raise callback
              (fn [exception]
                (update-progress
                 bus table-name (str "error (not supported by this bank)" (.getMessage exception)))
                (println
                 table-name
                 "exception message is: "
                 (.getMessage exception)
                 (when (= table-name "transactions")
                   exception))))))

(defn download-data
  [system bus]
  (get-data system bus {:table-name "info"
                        :id-key :full_name})
  (get-data system bus {:table-name "accounts"
                        :id-key :account_id})
  (get-data system bus {:table-name "cards"
                        :id-key :account_id}))

(defn authenticate-truelayer
  [ctx system bus]
  (sse/publish-global-event
   bus {:event :update-progress
        :body {:key :account-progress
               :value {:authentication "loading"
                       :fetch-info "pending auth"
                       :fetch-cards "pending auth"
                       :fetch-accounts "pending auth"
                       :fetch-transactions "pending vaild card or account"}}})
  (Thread/sleep 3000)
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
        _ (sse/publish-global-event
           bus {:event :update-progress
                :body {:key :account-progress
                       :value {:authentication (if access_token
                                                 "success"
                                                 "error")
                               :fetch-info "loading"}}})
        {:keys [crux.tx/tx-time]}
        (store-tokens system access_token refresh_token)
        {:keys [access-token]}
        (crux.utils/pull-tx system :truelayer-tokens tx-time)]
    (when (some? access-token)
      (download-data system bus))
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
  [id {:keys [system event-bus]}]
  (yada/resource
   {:id id
    :methods
    {:get
     {:produces {:media-type "application/json"}
      :response (fn [ctx]
                  (def bus event-bus)
                  (let [user (future (authenticate-truelayer ctx system event-bus))]
                    (merge
                     (:response ctx)
                     {:status 303
                      :headers {"Location" "/"}})))}}}))

(defmethod ig/init-key ::store
  [id {:keys [system]}]
  (yada/resource
   {:id id
    :access-control
    {:allow-origin #{"*"}
     :allow-credentials true
     :allow-methods #{:get :post}
     :allow-headers #{"*"}}
    :methods
    {:get
     {:produces {:media-type "application/json"}
      :response (fn [ctx]
                  (def system system)
                  (def db (crux/db system))
                  (let [db (crux/db system)
                        data (take 500 (crux.utils/query-pull-all system :table-name :transactions))]

                    (or
                     data
                     {:error "No data yet, please add an account"})))}
     :post
     {:produces {:media-type "application/json"}
      :consumes {:media-type "application/json"}
      :response (fn [ctx]
                  (def system system)
                  (def db (crux/db system))
                  (let [db (crux/db system)
                        data (take 500 (crux.utils/query-pull-all system :table-name :transactions))]

                    (or
                     data
                     {:error "No data yet, please add an account"})))}}}))
