(ns key-metrics.report
  (:require [key-metrics.db :as km-db]))

(ns key-metrics.core
  (:require [clojure.string :as str]
            [key-metrics.utils :as km-utils]
            [key-metrics.print :as km-print]
            [key-metrics.db :as km-db]))

;; ;=================================== settings ==================================

;; (def keys-per-hour 5000)

;; (def sitting-key-interval 100)
;; ;; how close (in seconds) should two keys be for you to be considered "at your desk"

;; (def typing-key-interval 5)
;; ;; how close (in seconds) should two keys be for you to be considered typing

;; (def break-interval (* 5 60))
;; ;; how many seconds should two keys be apart for you to be considered on a break

;; (def keys-per-day (* keys-per-hour 8))


;; ;================================== utilities ==================================

;; ;================================== file read ==================================







;; ;=============================== data collection ===============================


(defn part-hour [keys]
  (partition-by #(:hour %) keys))

;; (defn hourly-report [hour-collection]
;;   {:hour (get-in (last hour-collection) [:time :hour])
;;    :count (count hour-collection)})

;; (defn read-days [data]
;;   (partition-by #(.getDayOfWeek (:obj (:time %)))  data))

;; (defn get-epoch-difference [a b]
;;   (- (:epoch (:time a))
;;      (:epoch (:time b))))


;; ;=================================== analysis ==================================


;; (defn interval-map [a b]
;;   {:a (format-by-clock-time (:obj (:time a)))
;;    :b (format-by-clock-time (:obj (:time b)))
;;    :_dif (int (/ (get-epoch-difference b a) 60))
;;    :dif (get-epoch-difference b a)})

;; (defn get-key-intervals [keys interval]
;;   ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
;;   (let [intervals  (->> (map interval-map keys (subvec keys 1))
;;                         (filter #(> (:dif %) interval)))]
;;     intervals))

;; (defn sum-valid-keys [keys interval comp-fn]
;; ;; accumulate the interval difference as a running total if the difference is less than the specified interval 
;;   (second-to-hours (loop [i 1 c 0]
;;                      (if (= i (count keys))
;;                        c
;;                        (let [a (nth keys i)
;;                              b (nth keys (dec i))
;;                              dif (get-epoch-difference a b)
;;                              p (comp-fn dif interval)]
;;                          (recur (inc i) (+ c (if p dif 0))))))))

(defn create-hour-totals [keys]
  ;; taking a seq of key events with field :hour, return a vector of length 24 with each element being the count of keys for that hour
  (reduce (fn [acc key]
            (assoc acc (:hour key)
                   (inc (nth acc (:hour key)))))
          (vec (repeat 24 0)) keys))


;; (defn get-key-hours [day]
;;   ;; get the number of work hours per day based on estimated keys per work hour
;;   (float (/ (count day) keys-per-hour)))

;; (defn get-percent-for-day [d]
;;   (int (* 100 (/ (count d) keys-per-day))))

;; ;=================================== printing ==================================
;; (defn get-key-frequencies [data]
;;   (let [table-data (->> (map first data)
;;                         frequencies
;;                         (sort-by last)
;;                         vec)
;;         table (map  (fn [el]
;;                       {:key (first el)
;;                        :count (second el)}) table-data)]))


;; ;=================================== reports ===================================


;; (defn create-n-day-report [n k]
;;   ;; query the db, creating a report for n days, focusing on field k (ex: "perc-keys")
;;   (let [dates (->> (java.time.LocalDateTime/now)
;;                    (iterate #(.minusDays % 1))
;;                    (map #(.format % (get-formatter date-save-format)))
;;                    (take n))
;;         reports (km-db/get-reports dates)]
;;     reports))

;; (defn create-report [today today-hours]
;; ;; get report for one day in serializable format
;;   (let  [report {:keys (count today)
;;                  :time (get-epoch (java.time.LocalDateTime/now))
;;                  :clock-time (get-epoch (java.time.LocalDateTime/now))
;;                  :date (.format (java.time.LocalDateTime/now) (get-formatter date-save-format))
;;                  :perc-keys (get-percent-for-day today)
;;                  :key-hours (double (get-key-hours today))
;;                  :typing-hours (double (sum-valid-keys today typing-key-interval <))
;;                  :sitting-hours (double (sum-valid-keys today sitting-key-interval <))
;;                  :break-hours (get-key-intervals today break-interval)
;;                  :keys-this-hour (count (last today-hours))}]
;;     (println "saving...")
;;     (km-db/add-report report)
;;     report))

;; ;===================================== main ====================================


(defn decorate-raw-key-data [keys]
  (map #(assoc % :hour (km-utils/epoch-to-clock-hour (:epoch %))) keys))

(defn report [& args]
  (let [todays-raw-keys (km-db/get-key-events-for-day (km-utils/get-todays-record-date))
        todays-keys (decorate-raw-key-data todays-raw-keys)
        hour-totals (create-hour-totals todays-keys)
        ;; day-report (create-report today today-hours)
        ;; days (read-days data)
        ;; days-report (create-n-day-report 10 :perc-keys)
        ]
    ;; (println "hours" hour-totals)
    ;; (km-print/plot-n-days days-report :perc-keys)
    (km-print/plot-day hour-totals)
    ;; (km-print/print-break-report day-report)
    ;; (km-print/print-report day-report)
    ))

(report)


;; (utils/epoch-to-record-date 


;; (let []
  ;; (count todays-keys))


