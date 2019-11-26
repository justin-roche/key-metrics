
(ns key-metrics.core
  (:require [clojure.string :as str]
            [key-metrics.utils :as km-utils]
            [key-metrics.read :as km-read]
            [key-metrics.print :as km-print]
            [key-metrics.db :as km-db]
            [clojure.pprint :as pp]))

;; ;=================================== settings ==================================


(def keys-per-hour 5000)
(def sitting-key-interval 100)
;; how close (in seconds) should two keys be for you to be considered "at your desk"

(def typing-key-interval 5)
;; how close (in seconds) should two keys be for you to be considered typing

(def break-interval (* 5 60))
;; how many seconds should two keys be apart for you to be considered on a break

(def keys-per-day (* keys-per-hour 8))


;; ;=================================== analysis ==================================


(defn partition-hour [keys]
  (partition-by :hour  keys))

(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) keys-per-day))))

(defn sum-key-intervals [keys interval comp-fn field last-report]
  ;; accumulate the interval difference as a running total if the difference meets predicate comp-fn

  (if (= (count keys) 0) 0
      (let [start-index (if (or (nil? last-report) (nil? (get last-report field)))
                          1
                          (:total-keys last-report))
            start-count (if (or (nil? last-report) (nil? (get last-report field)))
                          0
                          (get last-report field))]

        (println "getting intervals for " field)
        (println "last rep start index" start-index)
        (println "last rep start count" start-count)
        (km-utils/second-to-hours (loop [i start-index c start-count]
                                    (if (= i (count keys))
                                      c
                                      (let [a (nth keys i)
                                            b (nth keys (dec i))
                                            dif (get-epoch-difference a b)
                                            p (comp-fn dif interval)]
                                        (recur (inc i) (+ c (if p dif 0))))))))))

(defn get-epoch-difference [a b]
  (- (:epoch a) (:epoch b)))

(defn interval-map [a b]
  {;; :_a (km-utils/epoch-to-hhmm (:epoch a))
   ;; :_b (km-utils/epoch-to-hhmm (:epoch b))
   :a (:epoch a)
   :b (:epoch b)
   :_dif (int (/ (get-epoch-difference b a) 60))
   :dif (get-epoch-difference b a)})

(defn accumulate-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria; recent keys are first in the list
  (let [intervals  (->> (map interval-map keys (subvec (vec keys) 1))
                        (filter #(> (:dif %) interval)))]
    (println "interval count " (count intervals))
    intervals))

(defn create-hour-totals [keys]
  ;; taking a seq of key events with field :hour, return a vector of length 24 with each element being the count of keys for that hour
  (reduce (fn [acc key]
            (assoc acc (:hour key)
                   (inc (nth acc (:hour key)))))
          (vec (repeat 24 0)) keys))

;===================================== main ====================================

(defn get-new-keys [keys last-report]
  ;; (print (type keys))
  (if (nil? last-report)
    keys
    (subvec keys (:total-keys last-report))))

(defn get-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
  (let [intervals  (->> (map interval-map keys (subvec keys 1))
                        (filter #(> (:dif %) interval)))]
    intervals))

(defn create-day-report [record-date]
;; get report for one day in serializable format
  (println "getting report for " record-date)
  (let  [keys (doall (km-db/get-key-events-for-day record-date))
         day-hours (partition-hour keys)
         last-report (km-db/get-report-for-day record-date)
         report {;;

                 :date record-date
                 :total-keys (count keys)
                 :perc-keys (get-percent-for-day keys)
                 :keys-this-hour (count (first day-hours))
                 :sitting-hours (double (sum-key-intervals (reverse keys) sitting-key-interval < :sitting-hours last-report))

                 :typing-hours (double (sum-key-intervals (reverse keys) typing-key-interval < :typing-hours last-report))
                 :break-hours (accumulate-key-intervals (reverse keys) break-interval)
                 :key-hours (double (get-key-hours keys))}]
    (newline)
    (println "keys length" (count keys))
    (pp/pprint report)
    (km-db/add-report-for-day record-date report)))

(defn get-days-reports []
  (let [all  (km-db/get-all-dates)]
    (println "all " (count all))
    (doall (map #(create-day-report %) all))))

(defn reset []
  (km-db/clear-db)
  (km-read/import-log))

(defn read-new []
  (km-read/import-log)
  (km-db/info))

;; (read-new)
;; (create-day-report "26-11-2019")
;; (km-db/info)
;; (reset)
(get-days-reports)


