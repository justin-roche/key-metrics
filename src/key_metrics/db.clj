(ns key-metrics.db
  (:require [key-metrics.dbpass :as pass]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.pprint :as pp]
            [key-metrics.utils :as km-utils]
            [clojure.string :as str]))

(def server1-conn {:pool {} :spec {:host "127.0.0.1"
                                   :port 6379
                                   :password pass/db-pass}})

(def dump-path "/Users/justin/ref/key-metrics/dumps/")

(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

;================================== key events =================================

(defn add-new-key-events [new-keys old-keys name]
  ;; if a record (key event sequence) already exists for the day, add only new keys to it; filter out old (potentially duplicated) keys from new keys in case something has gone wrong
  (let [p  (filter #(> (:epoch %) (:epoch (first old-keys)))  new-keys)
        l  (concat  p old-keys)]
    (println "adding " (count p) " keys")
    (wcar* (car/set name l))))

(defn add-new-key-event-seq [new-keys name]
  ;; add totally new key event seqs for an entire day
  (println "adding new event sequence for " name (count new-keys))
  (wcar* (car/set name new-keys)))

(defn update-key-event-seq [new-keys name]
  ;; add a key event sequence for day (first keys) if it does not exist, otherwise concat to existing value, filtering out keys that are duplicates (ie come before the last element of the existing sequence).
  (let [dbname (str "keys:" name)
        ;; new-keys (second keys)
        old-keys (wcar* (car/get dbname))]
    (do
      (cond
        (nil? old-keys) (add-new-key-event-seq  new-keys dbname)
        (< (count old-keys) (count new-keys)) (add-new-key-events  new-keys old-keys dbname)
        :else (println "skipping: " dbname)))))

(defn update-key-events [days]
  (newline)
  (doall (map
          (fn [name]
            (update-key-event-seq (get days name) name)) (keys days))))

(defn get-key-events-for-day [d]
  (wcar*
   (car/get (str "keys:" d))))

(defn get-all-dates []
  ;; get the list of all record dates for which there are key events
  (map #(str/replace % #"keys:" "") (wcar*
                                     (car/keys "keys:*"))))

;=================================== reports ===================================

(defn get-report-for-day [date]
  (wcar*
   (car/get (str "report:" date))))

(defn add-report-for-day [date report]
  (wcar*
   (car/set (str "report:" date) report)))

(defn get-reports-for-days [v]
  (let [reports (map #(get-report-for-day %) v)]
    reports))


;=============================== startup/shutdown ==============================


(defn shutdown-db []
  (let [ex (shell/sh "redis-cli" "shutdown" (str "-a" pass/db-pass))]
    (if (= 0 (:exit ex))
      (println "exited with 0"))))

(defn start-db []
  (let [ex (shell/sh "redis-server" "/Users/justin/redis.conf")]
    (if (= 0 (:exit ex))
      (println "exited with 0"))))

(defn clear-db []
  (wcar* (car/flushdb)))

;=================================== logging ===================================
(defn info []
  (newline)
  (let [all (get-keys "*")
        keys (wcar*
              (car/keys "keys:*"))
        counts (map #(count (wcar* (car/get %))) keys)
        t (map (fn [name count]
                 {:name name :count count}) keys counts)]
    (pp/print-table t)))

(defn get-keys [pattern]
  (wcar*
   (car/keys pattern)))

;===================================== dump ====================================

(defn get-key-data [k]
  (wcar* (car/get k)))

(defn dump-key-data [k data]
  (with-open [wrtr (clojure.java.io/writer (str dump-path "rm-" k ".json"))]
    (.write wrtr (json/write-str {:key k
                                  :data data}))))
(defn dump-db [keys]
  (let [keys (vec (get-keys keys))]
    (map #(dump-key-data % (get-key-data %)) keys)))

(info)
