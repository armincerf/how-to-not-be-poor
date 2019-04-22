(ns edge.sse.event-stream
  (:require
   [integrant.core :as ig]
   [clojure.tools.logging :as logging]
   [cognitect.transit :as transit]
   [manifold.bus :as bus]
   [manifold.deferred :as deferred]
   [manifold.stream :as stream]
   [yada.yada :as yada])
  (:import
   (java.io ByteArrayOutputStream)))

(derive :edge.sse.event-stream/event-bus :sse/bus)

(defn write-string
  [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out)))

(def message-marshal-xf
  (map write-string))

(defn audience-filter-xf
  [user]
  (filter #(if-let [aud (:audience %)]
             (aud user)
             %)))

(defn publish-global-event
  "Given a manifold bus which the event stream subscribes to, publishes 'event' as
  a global message."
  [bus event]
  (try
    (deferred/timeout! (bus/publish! bus :global event) 1000)
    (catch Exception e
      (logging/error e "error publishing global event"
                     {:event event
                      :bus bus}))))

(defmethod ig/init-key ::event-bus
  [_ _]
  (bus/event-bus))


(defmethod ig/init-key ::global-event-stream
  [id {:keys [user-id bus]}]
  (yada/resource
   {:id :route/events
    :produces [{:media-type "text/event-stream"}]
    :access-control {:allow-origin "*"
                     :allow-credentials true}
    :methods
    {:get
     {:response
      (fn event-stream
        [ctx]
        (let [s (stream/stream 50 (comp (audience-filter-xf user-id)
                                        message-marshal-xf))]
          (stream/connect (bus/subscribe bus :global) s)
          s))}}}))
