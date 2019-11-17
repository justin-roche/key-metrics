(ns key-metrics.core
  (:require [clojure.string :as str]))

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :hour (.getHour ldt)}))

(defn add-line [l]
  (let [entry  (str/split l #" :: ")]
    (vector (str/replace (first entry) #"\[|\]" "")
            (parse-date (last entry)))))

(defn read-file []
  (with-open [rdr (clojure.java.io/reader "/Users/justin/logfile.txt")]
    (doall (map add-line (filter #(> (count %) 0) (line-seq rdr))))))

(defn part-hour [els]
  (partition-by #(:hour (last %)) els))

(defn hourly-report [hour-collection]
  {:hour (:hour (last (last hour-collection)))
   :count (count hour-collection)})

(defn print-report [days last-hours keys-hour]
    (println "total days: " (count days))
    (println "keys today: " (count (last days)))
    (println "keys historical: " (map count days))
    (println "num hours today: " (count last-hours))
    (println "keys/hour: " keys-hour))

(defn get-data []
  (let [data (read-file)
        days  (partition-by #(.getDayOfWeek (:obj (last %)))  data)
        last-hours  (part-hour (last days))
        keys-hour (map hourly-report last-hours)]
    (print-report days last-hours keys-hour)
))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

;; (def job (set-interval get-data 60000))

(get-data)
