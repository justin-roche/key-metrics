(ns key-metrics.read
  (:require [clojure.string :as str]))

;; read log file into key event lists by day

(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")

(def date-save-format "dd-MM-YYYY")
(def date-read-format "E MMM dd HH:mm:ss yyyy")

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))
(def date-read-formatter (get-formatter date-read-format))

(defn get-epoch [ldt])

(defn raw-date-to-epoch [d]
  (.toEpochSecond (.atZone  (java.time.LocalDateTime/parse d date-read-formatter)
                            (java.time.ZoneId/systemDefault))))

(defn epoch-to-record-date [d]
  (.format (java.time.LocalDateTime/ofInstant
            ( java.time.Instant/ofEpochSecond d)
            (java.time.ZoneId/systemDefault))
           (get-formatter date-save-format)))

(defn read-line [l]
  ;; parse a single line of the log file; 
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))]
    {:key "."
     :epoch   (raw-date-to-epoch (last entry))}))

(defn read-log []
  (vec (with-open [rdr (clojure.java.io/reader log-path)]
         (doall (map read-line (filter #(> (count %) 0) (line-seq rdr)))))))

(defn read-days []
  (let [key-records (read-log)
        days (group-by #(epoch-to-record-date (:epoch %)) key-records)]
    (println "saving days:" (count days))
    ))

(read-days)
