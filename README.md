# high-c

A Clojure interface for communicating with 37signals' Highrise API.

## Install

Add to Lein's dependencies:

```
[[high-c "0.1.0"]]
```

## Usage

```clojure
(require '[high-c.core :as high])

(def auth {:domain "banzai.highrisehq.com"
           :token "abcd1234"})

(search high/company "Your Company")
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
