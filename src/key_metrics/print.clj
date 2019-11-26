(ns key-metrics.print
  (:require [clojure.pprint :as pp]
            [key-metrics.utils :refer :all]
            [repl-plot.core :as plot]))

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
               {:name  "date"
                :value (:date report)}]]
    (pp/print-table table)))

(defn print-break-report [report]
  (let [break-hours (:break-hours report)
        table (vec (map
                    (fn [i]
                  ;; (println "i: " i)
                      {:hour  (:a i)
                       :duration (:_dif i)})
                    break-hours))]
    (pp/print-table table)))

(defn plot-day [hours]
  (let [xs (mapv float (range 24))
        ys (mapv float hours)]
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

;; (defn log [& rest]
;;   (set! *print-length* 10)
;;   (newline)
;;   (newline)
;;   (apply (println s)))
