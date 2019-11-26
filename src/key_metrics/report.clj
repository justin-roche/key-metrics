
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


(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) keys-per-day))))

(defn sum-key-intervals [keys interval comp-fn]
  ;; accumulate the interval difference as a running total if the difference meets predicate comp-fn
  (if (= (count keys) 0) 0
      (km-utils/second-to-hours (loop [i 1 c 0]
                                  (if (= i (count keys))
                                    c
                                    (let [a (nth keys i)
                                          b (nth keys (dec i))
                                          dif (get-epoch-difference a b)
                                          p (comp-fn dif interval)]
                                      (recur (inc i) (+ c (if p dif 0)))))))))

(defn get-typing-hours [keys last-report]
  ;; add the new total sitting time to the previous one
  (if (nil? last-report)
    (do
      (double (sum-key-intervals keys typing-key-interval <)))
    (do
      (let [prev (:typing-hours last-report)
            prev-epoch (:time last-report)
            current-keys (vec (filter #(> (:epoch %) prev-epoch) keys))]
        (+ prev (double
                 (sum-key-intervals [] typing-key-interval <)))))))

(defn get-sitting-hours [keys last-report]
  (println "last report" last-report)
  ;; add the new total sitting time to the previous one
  (if (nil? last-report)
    (do
      (println "no previous hours")
      (double (sum-key-intervals keys sitting-key-interval <)))
    (do
      (let [prev (:sitting-hours last-report)
            prev-epoch (:time last-report)
            current-keys (vec (filter #(> (:epoch %) prev-epoch) keys))]
        ;; (println "prev" prev "ep" prev-epoch "current keys length" (count current-keys) "c keys type" (type current-keys))
        (+ prev (double
                 (sum-key-intervals [] sitting-key-interval <)))))))

(defn interval-map [a b]
  {:a (km-utils/epoch-to-hhmm (:epoch a))
   :b (km-utils/epoch-to-hhmm (:epoch b))
   :_dif (int (/ (get-epoch-difference b a) 60))
   :dif (get-epoch-difference b a)})

(defn accumulate-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
  ;; (println "type" (type keys))
  (let [intervals  (->> (map interval-map keys (subvec (vec keys) 1))
                        (filter #(> (:dif %) interval)))]
    intervals))

(defn create-hour-totals [keys]
  ;; taking a seq of key events with field :hour, return a vector of length 24 with each element being the count of keys for that hour
  (reduce (fn [acc key]
            (assoc acc (:hour key)
                   (inc (nth acc (:hour key)))))
          (vec (repeat 24 0)) keys))

;===================================== main ====================================

(defn decorate-raw-key-data [keys]
  (map #(assoc % :hour (km-utils/epoch-to-clock-hour (:epoch %))) keys))

(defn get-new-keys [keys last-report]
  ;; (print (type keys))
  (if (nil? last-report)
    keys
    (subvec keys (:keys-total last-report))))

(defn create-day-report [keys record-date]
;; get report for one day in serializable format
  (let  [todays-date (km-utils/get-todays-record-date)
         report {;;
                 :date record-date
                 :keys-total (count keys)
                 :perc-keys (get-percent-for-day keys)
                 :key-hours (double (get-key-hours keys))}]
    (pp/pprint report)
    (km-db/add-report-for-day (km-utils/get-todays-record-date) report)
    report))

(defn get-days-reports []
  (let [all  (km-db/get-all-key-events)]
    (doall (map (fn [day]
                  (create-day-report day
                                     (km-utils/epoch-to-record-date
                                      (:epoch (first day))))) all))))

(defn reset []
  (km-db/clear-db)
  (km-read/import-log)
  (report))

(defn read-new []
  (km-read/import-log)
  (km-db/info))

(read-new)
;; (km-db/info)
;; (reset)
(get-days-reports)

