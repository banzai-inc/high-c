# high-c

A Clojure interface for communicating with 37signals' Highrise API.

## Install

Add to Lein's dependencies:

```clojure
[[high-c "0.1.7"]]
```

## Usage

```clojure
(require '[high-c.core :as h])

(def auth {:domain "banzai.highrisehq.com"
           :token "abcd1234"})

(def companies (h/search h/company "Your Company" auth))

user=> (#high_c.core.Company{:id "66091540", :name "Your Company", :phone-number nil})

(h/url (first (companies)) auth)

user=> "https://banzai.highrisehq.com/companies/66091540"

(h/fetch-by-company h/person 1 auth)

user=> (#high_c.core.Person{...}

(h/fetch h/person 1 auth)
```

## License

Copyright © 2013 Banzai Inc.

Distributed under the Eclipse Public License, the same as Clojure.
