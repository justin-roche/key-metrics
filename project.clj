(defproject key-metrics "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[repl-plot "0.1.0-SNAPSHOT"]
                 [com.taoensso/carmine "2.19.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [clojurewerkz/quartzite "2.1.0"]
                 [org.clojure/data.json "0.2.7"]
                 [org.clojure/clojure "1.10.0"]]

  :main key-metrics.core
  :repl-options {:init-ns key-metrics.core})
