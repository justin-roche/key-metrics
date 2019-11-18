(ns key-metrics.db
  (:require [key-metrics.dbpass :as pass]
            [clojure.java.shell :as shell]
            [taoensso.carmine :as car :refer (wcar)]))

(def server1-conn {:pool {} :spec {:host "127.0.0.1"
                                   :port 6379
                                   :password pass/db-pass}})

(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn shutdown-db []
  (let [ex (shell/sh "redis-cli" "shutdown" (str "-a" pass/db-pass))]
    (if (= 0 (:exit ex))
      (println "exited with 0")
      (println "no 0"))))

(defn start-db []
  (let [ex (shell/sh "redis-server" "/Users/justin/redis.conf")]
    (if (= 0 (:exit ex))
      (println "exited with 0")
      (println "no 0"))))

(defn get-day [date]
  (wcar* (car/ping)
         (car/get "foo")))

(defn add-report [report]
  (wcar* (car/ping)
         (car/set (:date report) report)
         (car/get (:date report))))

(defn syncdb [d]
  (println (count d))
  (add-report d))

;; (conn)
