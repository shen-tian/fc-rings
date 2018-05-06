(defproject fc-rings "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [overtone/osc-clj "0.9.0"]
                 [quil "2.7.1"]]
  :profiles {:uberjar {:aot :all}}
  :main fc-rings.core)
