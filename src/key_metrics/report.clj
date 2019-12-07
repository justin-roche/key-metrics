
(ns key-metrics.report
  (:require [clojure.string :as str]
            [key-metrics.utils :as km-utils]
            [taoensso.carmine :as car :refer (wcar)]
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
;; minimum duration of a break; how many seconds should two keys be apart for you to be considered on a break

(def time-between-breaks (* 30 60))
;; the maximum time you can go without a break

(def keys-per-day (* keys-per-hour 8))


;; ;=================================== analysis ==================================


(defn get-epoch-difference [a b]
  (- (:epoch a) (:epoch b)))

(defn get-key-hours [day]
  ;; get the number of work hours per day based on estimated keys per work hour
  (float (/ (count day) keys-per-hour)))

(defn partition-hour [keys]
  (partition-by :hour  keys))

(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) keys-per-day))))

(defn iterate-key-intervals [keys start-index  comp-fn interval]
  (loop
   ;; [i 1 c 0]
   [i start-index c 0]
    (if (= i (count keys))
      (do
        (println "c" c)
        c)
      (let [a (nth keys i)
            b (nth keys (dec i))
            dif (get-epoch-difference a b)
            p (comp-fn dif interval)]
        (recur (inc i) (+ c (if p dif 0)))))))

(defn sum-key-intervals [keys interval comp-fn field last-report]
    ;; accumulate the interval difference as a running total if the difference meets predicate comp-fn. keys are all the keys for the day. If there is a last report with the field being accumulated, only start iterating at an index equal to the total keys of the last report. In the case this runs between db updates, ensure that the result value is higher than original, otherwise return original value.
  (if (= (count keys) 0) 0
      (let [;;
            no-field (nil? (get last-report field))
            ;; no-field true
            start-index (if (or (nil? last-report) no-field) 1
                            (:total-keys last-report))
            start-value-hours (if (or (nil? last-report) no-field) 0
                                  (get last-report field))
            start-value-seconds (km-utils/hours-to-seconds start-value-hours)
            additional-seconds  (iterate-key-intervals keys start-index comp-fn interval)
            total-seconds (+ additional-seconds start-value-seconds)]
        (println "start value seconds"  start-value-seconds "additional" additional-seconds)
        (if (<= total-seconds start-value-seconds)
          (do
            (println "no change, start value:" start-value-hours)
            start-value-hours)
          (do
            (println "r" (km-utils/second-to-hours total-seconds))
            (km-utils/second-to-hours total-seconds))))))

(defn interval-map [a b]
  ;; create an interval object with a as the early key, b the later key, dif the difference in epoch time, _dif the displayable difference in minutes
  {:a (:epoch a)
   :b (:epoch b)
   :_dif (int (/ (get-epoch-difference b a) 60))
   :dif (get-epoch-difference b a)})

(defn accumulate-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria (greater than interval); recent keys are first in the list
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

(defn get-new-keys [keys last-report]
  (if (nil? last-report)
    keys
    (subvec keys (:total-keys last-report))))

(defn get-hours-breakdown [day-hours]
  (map #(count %) day-hours))

(defn get-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
  (let [intervals  (->> (map interval-map keys (subvec keys 1))
                        (filter #(> (:dif %) interval)))]
    intervals))

(defn get-time-since-last-break [last-break]
  ;; get time since last break in ms
  (if (nil? last-break)
    nil
    (-  (km-utils/get-current-epoch) last-break)))

(defn get-has-new-keys [keys last-report]
  (println (:total-keys last-report) (count keys))
  (if (and (> (count keys) 0) (nil? last-report))
    true
    (> (count keys) (:total-keys last-report))))

(defn create-day-report [record-date]
;; get report for one day in serializable format
  (let  [keys (doall (km-db/get-key-events-for-day record-date))
         day-hours (partition-hour keys)
         last-report (km-db/get-report-for-day record-date)
         breaks-series (accumulate-key-intervals (reverse keys) break-interval)
         last-break  (:b (last breaks-series))
         time-since-last-break (get-time-since-last-break last-break)
         report {;;
                 :break-hours breaks-series
                 :last-break (if (nil? last-break) nil (km-utils/epoch-to-hhmm last-break))
                 :time-since-last-break (if (nil? last-break) nil time-since-last-break)
                 :break-due (if (nil? last-break) false (> time-since-last-break time-between-breaks))
                 ;;
                 :date record-date
                 :total-keys (count keys)
                 :has-new-keys (get-has-new-keys keys last-report)
                 :hours-breakdown (get-hours-breakdown day-hours)
                 :perc-keys (get-percent-for-day keys)
                 :keys-this-hour (count (first day-hours))
                 :sitting-hours (double (sum-key-intervals (reverse keys) sitting-key-interval < :sitting-hours last-report))
                 :typing-hours (double (sum-key-intervals (reverse keys) typing-key-interval < :typing-hours last-report))
                 :key-hours (double (get-key-hours keys))}]
    (km-db/add-report-for-day record-date report)))

(defn create-today-report []
  (create-day-report (km-utils/get-todays-record-date)))

(defn create-week-report [n]
  (let [dates (->> (java.time.LocalDateTime/now)
                   (iterate #(.minusDays % 1))
                   (map #(km-utils/ldt-to-record-date %))
                   (take n))
        reports (reverse (subvec (vec (km-db/get-reports-for-days dates)) 1))
        week {;;
              :days (count reports)
              :perc-keys-avg (double (/ (reduce (fn [acc x]
                                                  (+  (:perc-keys x) acc)) 0 (vec reports)) n))
              :sitting-hours-avg (/ (reduce (fn [acc x]
                                              (+  (:sitting-hours x) acc)) 0 (vec reports)) n)
              :sitting-hours-total (reduce (fn [acc x]
                                             (+  (:sitting-hours x) acc)) 0 (vec reports))
              :typing-hours-avg (/ (reduce (fn [acc x]
                                             (+  (:typing-hours x) acc)) 0 (vec reports)) n)}]
    (pp/pprint week)))

(defn create-days-reports []
  (let [all  (km-db/get-all-dates)]
    (doall (map #(create-day-report %) all))))

(create-today-report)
(km-print/print-report (km-db/get-report-for-day (km-utils/get-todays-record-date)))
;; (km-print/print-break-report (km-db/get-report-for-day (km-utils/get-todays-record-date)))
;; (km-print/plot-day (km-db/get-report-for-day (km-utils/get-todays-record-date)))
