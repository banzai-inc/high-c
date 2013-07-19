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

;; Constructors

(defn new-company
  "Constructor method for Company"
  [id name phone-number]
  (Company. id name phone-number))
 
(defprotocol Highrise
  (search [this q auth] "Search Highrise by item name"))

(defrecord Company [id name phone-number]
  Highrise
  (search [this q auth]
    (let [tree (search* "companies" q auth)
          companies (d/xml-> tree :company)]
      (for [company companies]
        (new-company (first (d/xml-> company :id d/text))
                     (first (d/xml-> company :name d/text))
                     (first (d/xml-> company :phone-number d/text)))))))

(def company (Company. nil nil nil))

;; (def banzai-auth {:domain "banzai.highrisehq.com"
;;                   :token "9c445b6df35b450bd3030ec129452686"})
;; 
;; (search company "Mountain America" banzai-auth)
