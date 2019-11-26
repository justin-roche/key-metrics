(ns key-metrics.read
  (:require [clojure.string :as str]
            [key-metrics.db :as km-db]
            [key-metrics.utils :as km-utils]
            [clojure.pprint :as pp]))

;; (def log-path "/Users/justin/test.txt")
(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")

(defn read-log-line [l]
  ;; parse a single line of the log file; 
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))
        epoch (km-utils/raw-date-to-epoch (last entry))]
    {:key (first entry)
     :epoch epoch
     :hour (km-utils/epoch-to-clock-hour epoch)
     ;;
     }))

(defn read-log-lines []
  ;; read the keylog one line at a time, filtering out empty lines
  (vec (with-open [rdr (clojure.java.io/reader log-path)]
         (doall (reverse (map read-log-line (filter #(> (count %) 0) (line-seq rdr))))))))

(defn import-log []
  ;; read the log and group it by days, adding results to db under the key(s): keys:dd-mm-Y
  (println "importing...")
  (let [key-records (read-log-lines)
        days (group-by #(km-utils/epoch-to-record-date (:epoch %)) key-records)]
    (set! *print-length* 50)
    (km-db/update-key-events days)))

(import-log)

;; (km-db/info)



;; (defn clear-logfile []
;;   (let [pw (clojure.java.io/writer log-path)]
;;     (.close pw)))


;; (clear-logfile)
