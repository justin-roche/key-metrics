(ns key-metrics.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [repl-plot.core :as plot]
            [key-metrics.db :as db]))

;=================================== settings ==================================

(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")
(def keys-per-hour 5000)

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))
(def date-read-format "E MMM dd HH:mm:ss yyyy")
;; the format for dates in the raw keylog

(def date-save-format "dd-MM-YYYY")
;; the format to store dates in the db

(def date-read-formatter (get-formatter date-read-format))
(def date-save-formatter (get-formatter date-save-format))

(def sitting-key-interval 100)
;; how close (in seconds) should two keys be for you to be considered "at your desk"

(def typing-key-interval 5)
;; how close (in seconds) should two keys be for you to be considered typing

(def break-interval (* 5 60))
;; how many seconds should two keys be apart for you to be considered on a break

(def keys-per-day (* keys-per-hour 8))


;================================== utilities ==================================


(defn get-epoch [ldt]
  (.toEpochSecond (.atZone  ldt (java.time.ZoneId/systemDefault))))

(defn format-by-clock-time [ldt]
  (.format ldt (get-formatter "hh:mm")))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :epoch (get-epoch ldt)
     :hour (.getHour ldt)}))

(defn get-epoch-difference [a b]
  (- (:epoch (:time a))
     (:epoch (:time b))))

(defn second-to-hours [s]
  (/ (/ s 60) 60))

;================================== file read ==================================

(defn add-line [l]
  ;; parse a single line of the log file; parse individual keys to remove surrounding elements
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))]
    {:key (str/replace (first entry) #"\[|\]" "")
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

;=================================== analysis ==================================
(defn get-key-intervals [keys interval]
  ;; accumulate the intervals between key events as a series if the difference meets a selected criteria
  (println (filter #(> (:dif %) interval)
                         (map (fn [a b] {:a (format-by-clock-time (:obj (:time a)))
                                         :b (format-by-clock-time (:obj (:time b)))
                                         :dif (get-epoch-difference b a)})
                              keys (subvec keys 1))))
  ;; (loop [i 1 c []]
  ;;   (if (= i (count keys))
  ;;     c
  ;;     (let [a (nth keys i)
  ;;           b (nth keys (dec i))
  ;;           dif (get-epoch-difference a b)
  ;;           p (comp-fn dif interval)]
  ;;       (recur (inc i) (if p (conj c dif) c))))))
  )
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

(defn print-report [report]
  (let [table [{:name  "time"
                :value (.format (java.time.LocalDateTime/now) (get-formatter "hh:mm"))}
               {:name  "keys "
                :value (:keys report) :target keys-per-day}
               {:name  "percentage keys "
                :value (:perc-keys report)}
               {:name  "key hours "
                :value (format "%.2f" (:key-hours report))}
               {:name  "sitting hours "
                :value (format "%.2f" (:sitting-hours report))}
               {:name  "typing hours "
                :value (format "%.2f" (:typing-hours report))}
               {:name  "keys this hour "
                :value (:keys-this-hour report)}
               {:name  "date"
                :value (:date report)}]]
    (pp/print-table table)
    ;; (println "current time: " )
    ))

(defn plot-day [hours]
  (let [xs (mapv float (range 24))
        ys (mapv float hours)]
    (plot/plot xs ys :max-height 10  :x-axis-display-step 5.0 :precision 0.0)))

(defn plot-n-days [reports k]
  ;; plot n days focusing on field k
  (let [xs (mapv float (range (count reports)))
        ys (reverse (map (fn [r]
                           (if r
                             (float (k r))
                             0)) reports))]
    (plot/plot xs ys :max-height 10  :x-axis-display-step 5.0 :precision 0.0)))

(defn get-frequencies [data]
  (let [table-data (->> (map first data)
                        frequencies
                        (sort-by last)
                        vec)
        table (map  (fn [el]
                      {:key (first el)
                       :count (second el)}) table-data)]
    (pp/print-table table)))


;=================================== reports ===================================


(defn create-n-day-report [n k]
  ;; create a report for n days, focusing on field k (ex: "perc-keys")
  (let [dates (->> (java.time.LocalDateTime/now)
                   (iterate #(.minusDays % 1))
                   (map #(.format % (get-formatter date-save-format)))
                   (take n))
        reports (db/get-reports dates)]
    (plot-n-days reports k)))

(defn create-report [today today-hours]
  ;; get report for one day in serializable format
  (println "got data")
  {:keys (count today)
   :time (get-epoch (java.time.LocalDateTime/now))
   :clock-time (get-epoch (java.time.LocalDateTime/now))
   :date (.format (java.time.LocalDateTime/now) (get-formatter date-save-format))
   :perc-keys (get-percent-for-day today)
   :key-hours (double (get-key-hours today))
   :typing-hours (double (sum-valid-keys today typing-key-interval <))
   :sitting-hours (double (sum-valid-keys today sitting-key-interval <))
   :break-hours (get-key-intervals today break-interval)
   :keys-this-hour (count (last today-hours))})

;===================================== main ====================================

(defn -main [& args]
  (let [data (read-file)
        days (read-days data)
        today  (vec (last days)) 
        today-hours (part-hour today)
        hour-totals (create-hour-totals today)
        report (create-report today today-hours)]
    (db/add-report report)
    (create-n-day-report 10 :perc-keys)
    (plot-day hour-totals)
    (print-report report)))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(-main)
