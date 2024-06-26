(ns api.util.system
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [malli.core :as m]
            [malli.error :as me]
            [aero.core :as aero]
            [integrant.core :as ig])
  (:import (clojure.lang IFn)
           (java.net ServerSocket)))


(def ^:private CONFIG-FILE-PATH "config.edn")


(def ^:private PROFILES
  "Available profiles for app config."
  #{:dev :test :prod})


; Add #ig/ref tag for reading integrant config from aero.
(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))


(defmethod aero/reader 'free-port
  [_ _ _value]
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))


(defn config
  "Return edn config with all variables set."
  [profile]
  {:pre [(contains? PROFILES profile)]}
  (-> CONFIG-FILE-PATH
      (io/resource)
      (aero/read-config {:profile profile})))


(defn at-shutdown
  "Add hook for shutdown system on sigterm."
  [system]
  (-> (Runtime/getRuntime)
      (.addShutdownHook
        (Thread. ^IFn (bound-fn []
                        (log/info "[SYSTEM] System is stopping...")
                        (ig/halt! system)
                        (shutdown-agents)
                        (log/info "[SYSTEM] System has been stopped."))))))


(defn validate-schema!
  "Validate data against schema and throw an error if data is not valid."
  [{:keys [schema data error-message]}]
  (some-> (m/explain schema data)
          (me/humanize)
          (#(throw (Exception.  (str error-message ": " %))))))
