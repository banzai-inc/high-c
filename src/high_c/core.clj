(ns high-c.core
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as d :only (attr text xml->)]))

(defn- highrise-url [auth]
  (str "https://" (:domain auth) "/"))

(defn- from-xml
  "Deserialization of XML into records"
  [content]
  (zip/xml-zip
    (xml/parse
      (java.io.ByteArrayInputStream.
        (.getBytes (:body content))))))

(defn- search*
  "Compose a basic search"
  [endpoint q auth]
  (from-xml (client/get (str (highrise-url auth)
                             endpoint "/search.xml?term=" q)
                        {:basic-auth [(:token auth) "X"]})))

(defprotocol Highrise
  (search [this q auth] "Search Highrise by item name"))

(defrecord Company []
  Highrise
  (search [this q auth]
    (let [tree (search* "companies" q auth)
          companies (d/xml-> tree :company)]
      (for [company companies]
        {:id (first (d/xml-> company :id d/text))
         :name (first (d/xml-> company :name d/text))
         :phone-number (first (d/xml-> company :phone-number d/text))}))))

(def company (Company.))
