(ns key-metrics.read
  (:require [clojure.string :as str]
            [key-metrics.db :as km-db]
            [key-metrics.utils :as km-utils]
            [key-metrics.print :as km-print]
            [clojure.pprint :as pp]))

;; (def log-path "/Users/justin/test.txt")
(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")

(defn read-log-line [l]
  ;; parse a single line of the log file; 
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map #(str/replace % "  " " "))
                    (map str/trim))
        epoch (km-utils/raw-date-to-epoch (last entry))]
    {:key (first entry)
     :epoch epoch
     :hour (km-utils/epoch-to-clock-hour epoch)}))

(defn read-log-lines []
  ;; read the keylog one line at a time, filtering out empty lines
  (vec (with-open [rdr (clojure.java.io/reader log-path)]
         (doall (reverse (map read-log-line (filter #(> (count %) 0) (line-seq rdr))))))))

(defn clear-logfile []
  (with-open [w (clojure.java.io/writer  log-path)]
    (.write w (str ""))))

(defn import-log []
  ;; read the log and group it by days, adding results to db under the key(s): keys:dd-mm-Y
  (let [line-records (read-log-lines)
        has-pulse (> (count line-records) 0)
        key-records  (filter #(not= (:key %) "PULSE") line-records)
        days (group-by #(km-utils/epoch-to-record-date (:epoch %)) key-records)]
    (println "line records " (count line-records))
    (println  "key records " (count key-records))
    (if (not has-pulse)
      (do
        (println "no pulse")
        (km-print/triple-alert)
        (println "killing keylogger")
        (km-utils/kill-keylogger)
        (Thread/sleep 1000)
        (println "starting keylogger")
        (km-utils/start-keylogger)
        (Thread/sleep 1000)
        ))
    (clear-logfile)
    (km-db/update-key-events days)))

;; (import-log)

