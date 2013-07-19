(ns high-c.core
  (:require [clj-http.client :as client]))

(defprotocol Highrise
  (search [this q auth] "Search highrise"))

(defrecord Company []
  Highrise
  (search [this q auth]
    q))

(def company (Company.))
