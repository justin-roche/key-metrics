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
  ;; read the keylog one line at a time, filtering out empty lines
  (vec (with-open [rdr (clojure.java.io/reader log-path)]
         (doall (map read-log-line (filter #(> (count %) 0) (line-seq rdr)))))))

(defn read-days []
  ;; read the log and group it by days, adding results to db under the key(s): keys:dd-mm-yy
  (let [key-records (read-log)
        days (group-by #(epoch-to-record-date (:epoch %)) key-records)]
    (println "key records" (count key-records))
    (println "days" (count days))
    (db/update-key-events days)
    (db/info)))

;; (defn clear-logfile []
;;   (let [pw (clojure.java.io/writer log-path)]
;;     (.close pw)))


(read-days)



;; (clear-logfile)
