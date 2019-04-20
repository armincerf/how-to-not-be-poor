(ns how-to-not-be-poor.dashboard.frontend.ajax
  (:require [ajax.core :as ajax]))

(defn get-request
  [uri on-success on-error]
  {:http-xhrio {:method          :get
                :uri             uri
                :format          (ajax/json-request-format)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      on-success
                :on-failure      on-error}})

(defn put-request
  [uri params on-success on-error]
  {:http-xhrio {:method          :put
                :uri             uri
                :params          params
                :format          (ajax/json-request-format)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      on-success
                :on-failure      on-error}})

(defn post-request
  [uri params on-success on-error]
  {:http-xhrio {:method          :post
                :uri             uri
                :params          params
                :format          (ajax/json-request-format)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      on-success
                :on-failure      on-error}})

(defn delete-request
  [uri on-success on-error]
  {:http-xhrio {:method          :delete
                :uri             uri
                :format          (ajax/json-request-format)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      on-success
                :on-failure      on-error}})
