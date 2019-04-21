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

(def store (atom {:tokens {:access-token nil
                           :refresh-token nil}
                  :data nil}))

(def creds (edn/read-string (slurp (io/resource "credentials.edn"))))

(def base-uri "https://api.truelayer.com/data/v1/")

(defn renew-token!
  [refresh-token]
  (let [{:keys [access_token refresh_token] :as response}
        (http/post "https://auth.truelayer.com/connect/token"
                   {:form-params
                    (merge creds
                           {:grant_type "refresh_token"
                            :refresh_token refresh-token})
                    :as :json})]
    (prn "refreshing token" refresh-token)
    (when access_token
      (swap! store #(assoc % :tokens {:access-token access_token
                                      :refresh-token refresh_token})))
    (some? access_token)))

(defn load-transactions
  [account-id access-token]
  (http/get (str "https://api.truelayer.com/data/v1/cards/"
                 account-id "/transactions")
            {:headers {"Authorization" (str "Bearer " access-token)}
             :as :json
             :async? true}
            (fn [response] 
              (swap! store #(assoc-in % [:data "transactions"]
                                      (concat (or (get-in @store [:data "transactions"]) [])
                                              (-> response
                                                  :body
                                                  :results)))))
            (fn [exception]
              (println "exception message is: "
                       (.getMessage exception)))))

(defn data-api-request
  [system uri-stub]
  (let [full-uri (str "https://api.truelayer.com/data/v1/" uri-stub)
        {:keys [access-token refresh-token]} (:tokens @store)]
    (http/get full-uri
              {:headers {"Authorization" (str "Bearer " access-token)}
               :as :json
               :async? true}
              ;; respond callback
              (fn [response]
                (crux.api/submit-tx
                 system
                 [[:crux.tx/put :data
                   (merge
                    {:crux.db/id :data} (-> response
                                            :body
                                            :results))]])
                (swap! store #(assoc-in % [:data uri-stub]
                                        (concat (or (get-in @store [:data uri-stub]) [])
                                                (-> response
                                                    :body
                                                    :results))))
                (when (= "cards" uri-stub)
                  (doseq [card (-> response :body :results)]
                    (prn "doing" card)
                    (let [account-id (:account_id card)]
                      (load-transactions account-id access-token)))))
              ;; raise callback
              (fn [exception]
                (println "exception message is: "
                                       (.getMessage exception))
                (prn exception)))))

(defn get-info
  [system]
  (let [uri (str base-uri "info")
        {:keys [access-token refresh-token]} (:tokens @store)]
    (http/get uri
              {:headers {"Authorization" (str "Bearer " access-token)}
               :as :json
               :async? true}
              ;; respond callback
              (fn [response]
               (prn "transacting") 
                (prn "transacted" (crux.api/submit-tx
                                   system
                                   (vec (for [result (-> response
                                                         :body
                                                         :results)
                                              :let [id (crux.utils/str->key
                                                        (:full_name result))]]
                                          (do (prn "id = " id)
                                              [:crux.tx/put id
                                               (merge
                                                {:crux.db/id id
                                                 :table-name :info}
                                                result)
                                               (clojure.instant/read-instant-date
                                                (:update_timestamp result))]))))))
              ;; raise callback
              (fn [exception]
                (println "exception message is: "
                         (.getMessage exception))
                (prn exception)))))

(defn download-data
  [system]
  (get-info system))

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
                 :as :json}))]
    (when access_token
      (swap! store #(assoc % :tokens {:access-token access_token
                                      :refresh-token refresh_token}))
      (download-data system))
    (some? access_token)))

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
