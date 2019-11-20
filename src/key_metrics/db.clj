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

(defn update-key-event-seq [keys]
  ;; add a key event sequence for day (first keys) if it does not exist, otherwise concat to existing value
  (let [name (str "keys-" (first keys))
        cur (wcar* (car/get name))]
    (if cur
      (wcar* (car/set name (concat cur (second keys))))
      (wcar* (car/set name (concat [] (second keys)))))))

(defn update-key-events [days]
  (for [s  days]
    (update-key-event-seq s)))

(defn get-report [date]
  (wcar*
   (car/get date)))

(defn add-report [report]
  (wcar*
   (car/set (:date report) report)
   (car/get (:date report))))

(defn get-reports [v]
  (let [reports (map #(get-report %) v)]
    reports))



;; (conn)
