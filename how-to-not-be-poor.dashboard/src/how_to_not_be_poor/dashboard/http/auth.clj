(ns how-to-not-be-poor.dashboard.http.auth
  (:require
   [integrant.core :as ig]
   [schema.core :as s]
   [clj-http.client :as http]
   [yada.yada :as yada]))

(defn authenticate-truelayer
  [ctx]
  (def ctx ctx)
  (http/post "https://auth.truelayer.com/connect/token"
              {:form-params {:grant_type "authorization_code"
                             :client_id "x"
                             :redirect_uri "http://localhost:7979/#"
                             :client_secret "x"
                             :code (get-in ctx [:parameters :query "code"])}}
              {:async? true}
              ;; respond callback
              (fn [response] (println "response is:" response))
              ;; raise callback
              (fn [exception] (println "exception message is: " (.getMessage exception)))))

(defmethod ig/init-key ::login
  [id _]
  (yada/resource
   {:id id
    :methods
    {:post
     {:consumes {:media-type "application/json"}
      :produces {:media-type "application/json"}
      :response (fn [ctx]
                  (let [user (authenticate-truelayer ctx)]
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
  [id _]
  (yada/resource
   {:id id
    :methods
    {:get
     {:produces {:media-type "application/json"}
      :response (fn [ctx]
                  (prn "dstuff")
                  (let [user (authenticate-truelayer ctx)]
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
