(ns key-metrics.read
  (:require [clojure.string :as str]
            [key-metrics.utils :refer :all]
            [key-metrics.db :as db]))

;; (def log-path "/Users/justin/test.txt")
(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")

(defn read-log-line [l]
  ;; parse a single line of the log file; 
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))]
    {:key "."
     :epoch   (raw-date-to-epoch (last entry))}))

(defn read-log []
  (vec (with-open [rdr (clojure.java.io/reader log-path)]
         (doall (map read-log-line (filter #(> (count %) 0) (line-seq rdr)))))))

(defn read-days []
  (let [key-records (subvec (read-log) 100 1000)
        days (group-by #(epoch-to-record-date (:epoch %)) key-records)]
    (println "count: " (count key-records))
    (println "days count: " (count days))
    (db/update-key-events days)
    (db/info)))

;; (defn clear-logfile []
;;   (let [pw (clojure.java.io/writer log-path)]
;;     (.close pw)))


(read-days)



;; (clear-logfile)
