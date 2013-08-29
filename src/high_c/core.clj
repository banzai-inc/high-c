(ns high-c.core
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as d :only (attr text xml->)]))

(defn- highrise-url [auth]
  (str "https://" (:domain auth) "/"))

(def ^{:private true} company-endpoint "companies")
(def ^{:private true} people-endpoint "people")

(defn- basic-auth
  "Basic authentication headers"
  [auth]
  {:basic-auth [(:token auth) "X"]})

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
  (from-xml
    (client/get (str (highrise-url auth)
                     endpoint "/search.xml?term=" q)
                (basic-auth auth))))

(defn- fetch*
  "Compose a basic fetch"
  [endpoint id auth]
  (from-xml 
    (client/get (str (highrise-url auth) endpoint "/" id ".xml")
                (basic-auth auth))))

(defn- fetch-by-company*
  [endpoint id auth]
  "Compose fetch collection by company."
  (from-xml 
    (client/get (str (highrise-url auth) company-endpoint "/"
                     id "/" endpoint ".xml")
                (basic-auth auth))))

(defn- new-entities
  [tree f entity]
  (map f (d/xml-> tree entity)))

(defn- grab
  [tree el]
  (first (d/xml-> tree el d/text)))

(defprotocol HighriseItem
  (url [this auth] "Return URL for Highrise item.") 
  (search [this q auth] "Search Highrise by item name.")
  (fetch [this id auth] "Get Highrise entity by id.")
  (fetch-by-company [this id auth] "Get Highrise collection of entities by company."))

(declare ->Company)
(declare ->Person)

(defn new-company
  [tree]
  (merge
    (->Company)
    {:id (Integer/parseInt (grab tree :id))
     :name (grab tree :name)
     :phone-number (grab tree :phone-number)}))

(defrecord Company []
  HighriseItem
  (url [this auth]
    (str (highrise-url auth) company-endpoint "/" (.id this)))
  (search [_ q auth]
    "Search companies. Returns a vector of companies."
    (new-entities (search* company-endpoint q auth) new-company :company))
  (fetch [_ id auth]
    (new-company (fetch* company-endpoint id auth))))

(def company (->Company))

(defn new-person
  [tree]
  (merge
    (->Person)
    {:id (Integer/parseInt (grab tree :id))
     :first-name (grab tree :first-name)   
     :last-name (grab tree :last-name)   
     :contact-data {:email-addresses (for [ea (d/xml-> tree :contact-data :email-addresses :email-address)]
                                       {:email-address {:id (Integer/parseInt (grab ea :id))
                                                        :address (grab ea :address)
                                                        :location (grab ea :location)}})}}))

(defrecord Person []
  HighriseItem
  (fetch-by-company [_ id auth]
    (new-entities (fetch-by-company* people-endpoint id auth) new-person :person)))

(def person (->Person))

;; (fetch-by-company (->Person) (:id c) banzai-auth)
;; 
;; (require '[environ.core :refer :all])
;; (def banzai-auth {:domain (env :highrise-domain)
;;                   :token (env :highrise-token)})
;; (def c (first (search company "Mountain America" banzai-auth)))
