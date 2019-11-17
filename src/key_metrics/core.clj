(ns key-metrics.core
  (:require [clojure.string :as str]))
;; (use  'clojure.java.io)

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))

(defn parse-date [d]
  (let [formatter  (java.time.format.DateTimeFormatter/ofPattern "E MMM dd HH:mm:ss yyyy")
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
 ;; (println "x" (first hour-collection)) 
  {:hour (:hour (last (last hour-collection)))
   :count (count hour-collection)})

(defn foo
  []
  (let [data (read-file)
        days  (partition-by #(.getDayOfWeek (:obj (last %)))  data)
        last-hours  (part-hour (last days))
        keys-hour (map hourly-report last-hours)]
    (println "total days: " (count days))
    (println "keys today: " (count (last days)))
    (println "keys historical: " (map count days))
    (println "num hours today: " (count last-hours))
    (println "k/hour: " keys-hour)))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

;; (def job (set-interval foo 10000))

(foo)
