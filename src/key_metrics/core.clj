(ns key-metrics.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(def log-path "/Users/justin/logfile.txt")
(def hour-keys 3500)

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
  (with-open [rdr (clojure.java.io/reader log-path)]
    (doall (map add-line (filter #(> (count %) 0) (line-seq rdr))))))

(defn part-hour [els]
  (partition-by #(:hour (last %)) els))

(defn hourly-report [hour-collection]
  {:hour (:hour (last (last hour-collection)))
   :count (count hour-collection)})

(defn get-hours-worked [day]
  (int (/ (count day) hour-keys)))

(defn print-report [days]
  (let [obj [{:name  "total days" :value (count days)}
             {:name  "keys today" :value (count (last days))}
             {:name  "hours today" :value (get-hours-worked (last days))}
             {:name  "keys this hour" :value (count (last (part-hour (last days))))}]]
    (pp/print-table obj)))

(defn get-data []
  (let [data (read-file)
        days  (partition-by #(.getDayOfWeek (:obj (last %)))  data)]
    (print-report days)))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(get-data)
;; (def job (set-interval get-data 60000))
