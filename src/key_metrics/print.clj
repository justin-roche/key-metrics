(ns key-metrics.print
  (:require [clojure.pprint :as pp]
            [key-metrics.utils :refer :all]
            [repl-plot.core :as plot]
            [key-metrics.utils :as km-utils]))

(defn print-report [report]
  (let [table [{:name  "time"
                :value (.format (java.time.LocalDateTime/now) (get-formatter "hh:mm"))}
               {:name  "keys "
                :value (:total-keys report) :target (:keys-per-day report)}
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
               {:name  "time since last break "
                :value (int (/ (:time-since-last-break report) 60))}
               {:name  "date"
                :value (:date report)}]]
    (pp/print-table table)
    (println (:hours-breakdown report))))

(defn print-break-report [report]
  (let [break-hours (:break-hours report)
        table (vec (map
                    (fn [i]
                      {:hour  (epoch-to-hhmm (:a i))
                       :duration (:_dif i)})
                    break-hours))]
    (pp/print-table table)))

(defn plot-day [report]
  (let [xs (mapv float (range 24))
        ys (mapv float (:hours-breakdown report))]
    (plot/plot xs ys :max-height 10  :x-axis-display-step 5.0 :precision 0.0)))

(defn plot-n-days [reports k]
  ;; plot n days focusing on field k
  (let [xs (mapv float (range (count reports)))
        ys (map (fn [r]
                  (if r
                    (float (k r))
                    0)) reports)]
    (println "             n days report:")
    (plot/plot xs ys :max-height 10  :x-axis-display-step 5.0 :precision 0.0)))
