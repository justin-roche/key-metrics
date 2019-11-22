(ns key-metrics.core
  (:require [clojure.string :as str]
            [key-metrics.utils :refer :all]
            [key-metrics.print :as km-print]
            [key-metrics.db :as km-db]))

;=================================== settings ==================================

(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")
(def keys-per-hour 5000)

(def sitting-key-interval 100)
;; how close (in seconds) should two keys be for you to be considered "at your desk"

(def typing-key-interval 5)
;; how close (in seconds) should two keys be for you to be considered typing

(def break-interval (* 5 60))
;; how many seconds should two keys be apart for you to be considered on a break

(def keys-per-day (* keys-per-hour 8))


;================================== utilities ==================================

;================================== file read ==================================


(defn add-line [l]
  ;; parse a single line of the log file; parse individual keys to remove surrounding elements
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))]
    {:key "."
     :time (parse-date (last entry))}))

(defn read-file []
  (with-open [rdr (clojure.java.io/reader log-path)]
    (doall (map add-line (filter #(> (count %) 0) (line-seq rdr))))))


;=============================== data collection ===============================


(defn part-hour [keys]
  (partition-by #(:hour (:time %)) keys))

(defn hourly-report [hour-collection]
  {:hour (get-in (last hour-collection) [:time :hour])
   :count (count hour-collection)})

(defn read-days [data]
  (partition-by #(.getDayOfWeek (:obj (:time %)))  data))

(defn get-epoch-difference [a b]
  (- (:epoch (:time a))
     (:epoch (:time b))))


;=================================== analysis ==================================


(defn interval-map [a b]
  {:a (format-by-clock-time (:obj (:time a)))
   :b (format-by-clock-time (:obj (:time b)))
   :_dif (int (/ (get-epoch-difference b a) 60))
   :dif (get-epoch-difference b a)})

(defn get-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
  (let [intervals  (->> (map interval-map keys (subvec keys 1))
                        (filter #(> (:dif %) interval)))]
    intervals))

(defn sum-valid-keys [keys interval comp-fn]
;; accumulate the interval difference as a running total if the difference is less than the specified interval 
  (second-to-hours (loop [i 1 c 0]
                     (if (= i (count keys))
                       c
                       (let [a (nth keys i)
                             b (nth keys (dec i))
                             dif (get-epoch-difference a b)
                             p (comp-fn dif interval)]
                         (recur (inc i) (+ c (if p dif 0))))))))

(defn create-hour-totals [raw-hours]
  (loop [i 0 v (vec (repeat 24 0))]
    (if (= i (count raw-hours))
      v
      (let [h (get-in (nth raw-hours i) [:time :hour])]
        (recur (inc i) (assoc v h (inc (nth v h))))))))

(defn get-key-hours [day]
  ;; get the number of work hours per day based on estimated keys per work hour
  (float (/ (count day) keys-per-hour)))

(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) keys-per-day))))

;=================================== printing ==================================
(defn get-key-frequencies [data]
  (let [table-data (->> (map first data)
                        frequencies
                        (sort-by last)
                        vec)
        table (map  (fn [el]
                      {:key (first el)
                       :count (second el)}) table-data)]))


;=================================== reports ===================================


(defn create-n-day-report [n k]
  ;; query the db, creating a report for n days, focusing on field k (ex: "perc-keys")
  (let [dates (->> (java.time.LocalDateTime/now)
                   (iterate #(.minusDays % 1))
                   (map #(.format % (get-formatter date-save-format)))
                   (take n))
        reports (km-db/get-reports dates)]
    reports))

(defn create-report [today today-hours]
;; get report for one day in serializable format
  (let  [report {:keys (count today)
                 :time (get-epoch (java.time.LocalDateTime/now))
                 :clock-time (get-epoch (java.time.LocalDateTime/now))
                 :date (.format (java.time.LocalDateTime/now) (get-formatter date-save-format))
                 :perc-keys (get-percent-for-day today)
                 :key-hours (double (get-key-hours today))
                 :typing-hours (double (sum-valid-keys today typing-key-interval <))
                 :sitting-hours (double (sum-valid-keys today sitting-key-interval <))
                 :break-hours (get-key-intervals today break-interval)
                 :keys-this-hour (count (last today-hours))}]
    (println "saving...")
    (km-db/add-report report)
    report))

;===================================== main ====================================

(defn -main [& args]
  (let [data (read-file)
        days (read-days data)
        today  (vec (last days))
        today-hours (part-hour today)
        hour-totals (create-hour-totals today)
        day-report (create-report today today-hours)
        days-report (create-n-day-report 10 :perc-keys)]

    (km-print/plot-n-days days-report :perc-keys)
    (km-print/plot-day hour-totals)
    (km-print/print-break-report day-report)
    (km-print/print-report day-report)))

(-main)
