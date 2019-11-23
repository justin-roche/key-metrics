(ns key-metrics.db
  (:require [key-metrics.dbpass :as pass]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]
            [taoensso.carmine :as car :refer (wcar)]))

(def server1-conn {:pool {} :spec {:host "127.0.0.1"
                                   :port 6379
                                   :password pass/db-pass}})

(def dump-path "/Users/justin/ref/key-metrics/dumps/")

(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))


;================================== key events =================================


(defn update-key-events [days]
  (doseq [x days]
    (update-key-event-seq x)))

(defn update-key-event-seq [keys]
  ;; add a key event sequence for day (first keys) if it does not exist, otherwise concat to existing value
  (println "updating db for day: " (first keys) " with count : " (count keys))
  (let [name (str "keys:" (first keys))
        cur (wcar* (car/get name))]
    (if cur
      (wcar* (car/set name (concat cur (second keys))))
      (wcar* (car/set name (concat [] (second keys)))))))

(defn get-key-events-for-day [d]
  (wcar*
   (car/get (str "keys:" d))))

;=================================== reports ===================================


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


;============================= db general functions ============================


(defn get-keys [pattern]
  (wcar*
   (car/keys pattern)))





;=============================== startup/shutdown ==============================


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

(defn clear-db []
  (wcar* (car/flushdb)))

;=================================== logging ===================================
(defn info []
  (println "    db info:   ")
  (let [all (get-keys "*")
        keys (wcar*
              (car/keys "keys:*"))
        counts (map #(count (wcar* (car/get %))) keys)]
    (println "all: " all)
    (println "keys: " keys)
    (println "counts: " counts)
    (println "total keys recorded: " (reduce + counts))))

(defn get-key-data [k]
  (wcar* (car/get k)))

(defn dump-key-data [k data]
  (with-open [wrtr (clojure.java.io/writer (str dump-path "rm-" k ".json"))]
    (.write wrtr (json/write-str {:key k
                                  :data data}))))

(defn dump-db [keys]
  (let [keys (vec (get-keys keys))]
    (map #(dump-key-data % (get-key-data %)) keys)))

;; (dump-db "*")

;; (clear-db)
(info)
