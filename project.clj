(defproject high-c "0.1.5"
  :description "Interface for communicating with 37signals' Highrise API"
  :url "https://github.com/banzai-inc/high-c"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.5"]
                 [org.clojure/data.zip "0.1.1"]]
  :profiles {:dev {:dependencies [[environ "0.4.0"]]}})
