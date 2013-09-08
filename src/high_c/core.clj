(ns high-c.core
  (:import [java.io StringWriter])
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.xml :as dxml]
            [clojure.data.zip.xml :as d :only (attr text xml->)]))

(defn- highrise-url [auth]
  (str "https://" (:domain auth) "/"))

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

(defn- new-entities
  [tree f entity]
  (map f (d/xml-> tree entity)))

(defn- grab
  [tree el]
  (first (d/xml-> tree el d/text)))

(declare ->Company)
(declare ->Person)

(defmacro defnew
  [n t]
  `(defn ~n [attrs#]
     (merge (~t) attrs#)))

(defnew new-company ->Company)
(defnew new-person ->Person)

(defprotocol HighriseItem
  (sym [this] "Symbol representing root XML element.")
  (url [this auth] "Return URL for Highrise item.") 
  (endpoint [this] "Return entity's endpoint.")
  (hmap [this tree] "Map XML to entity.")
  (rmap [this] "Map entity to XML."))

(defrecord Company []
  HighriseItem
  (sym [_] :company)
  (hmap [_ tree]
    {:id (Integer/parseInt (grab tree :id))
     :name (grab tree :name)})
  (rmap [this]
    (dxml/element
      (sym this) {}
      (dxml/element :name {} (:name this))
      (when (:contact-data this)
        (dxml/element :contact-data {}
                      (dxml/element :phone-numbers {}
                                    (apply #(dxml/element :phone-number {}
                                                          (dxml/element :number {} (:number %))
                                                          (dxml/element :location {} (:location %)))
                                           (map :phone-number (:phone-numbers (:contact-data this)))))))))
  (endpoint [_] "companies")
  (url [this auth]
    (str (highrise-url auth) (endpoint this) "/" (:id this))))

(defrecord Person []
  HighriseItem
  (sym [_] :person)
  (endpoint [_] "people") 
  (hmap [_ tree]
    {:id (Integer/parseInt (grab tree :id))
     :first-name (grab tree :first-name)   
     :last-name (grab tree :last-name)   
     :contact-data {:email-addresses (for [ea (d/xml-> tree :contact-data :email-addresses :email-address)]
                                       {:email-address {:id (Integer/parseInt (grab ea :id))
                                                        :address (grab ea :address)
                                                        :location (grab ea :location)}})}}))

(def person (->Person))

(def company (->Company))

(defn fetch
  "Compose a basic fetch"
  [entity id auth]
  (merge
    entity
    (hmap entity
          (from-xml 
            (client/get (str (highrise-url auth)
                             (endpoint entity) "/" id ".xml")
                        (basic-auth auth))))))

(defn fetch-by-company
  [entity id auth]
  "Compose fetch collection by company."
  (new-entities
    (from-xml 
      (client/get (str (highrise-url auth) (endpoint (->Company)) "/" id "/" (endpoint entity) ".xml")
                  (basic-auth auth)))
    #(merge entity (hmap entity %))
    (sym entity)))

(defn search
  "Compose a basic search."
  [entity q auth]
  (new-entities
    (from-xml
      (client/get (str (highrise-url auth) (endpoint entity) "/search.xml?term=" q)
                  (basic-auth auth)))
    #(merge entity (hmap entity %))
    (sym entity)))

(defn- write-headers
  [auth body]
  (merge (basic-auth auth)
         {:body body
          :content-type "application/xml"}))

(defn create
  "Creates entity."
  [entity auth]
  (let [body (dxml/emit-str (rmap entity))]
    (merge
      entity
      (hmap entity
            (from-xml
              (client/post (str (highrise-url auth) (endpoint entity) ".xml")
                           (write-headers auth body)))))))

;; (require '[environ.core :refer :all])
;; (def banzai-auth {:domain (env :highrise-domain)
;;                   :token (env :highrise-token)})
;; (def c (first (search (->Company) "Mountain America" banzai-auth)))
;; (url c banzai-auth)
;; (fetch-by-company (->Person) 66091540 banzai-auth)
;; (fetch (->Person) 115622218 banzai-auth)
;; (create (new-company {:name "Hometown Credit Union (Test 1)"}) banzai-auth)
;; (try
;;   (create (new-company {:name "Hometown Credit Union (Test 4)"
;;                         :contact-data {:phone-numbers [{:phone-number {:number "333-333-3331"
;;                                                                        :location "Work"}}]}})
;;         banzai-auth)
;;   (catch Exception e
;;     (println (.toString e))))
