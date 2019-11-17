(ns key-metrics.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")
(def hour-keys 3500)
(def day-hours (* hour-keys 8))

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :hour (.getHour ldt)}))

(defn add-line [l]
  ;; parse a single line of the log file; parse individual keys to remove surrounding elements
  (let [entry  (->> (re-pattern log-file-delimiter)
                (str/split l)
                (map str/trim))]
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

(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) day-hours))))

(defn print-report [days]
  (let [today (last days)
        table [{:name  "total days" :value (count days)}
               {:name  "keys today" :value (count today)}
               {:name  "percent today" :value (get-percent-for-day today)}
               {:name  "hours today" :value (get-hours-worked today)}
               {:name  "keys this hour" :value (count (last (part-hour today)))}]]
    (pp/print-table table)))

(defn get-frequencies [data]
  (let [table-data (->> (map first data)
                        frequencies
                        (sort-by last)
                        ;; reverse
                        vec)
        table (map  (fn [el]
                      {:key (first el)
                       :count (second el)}) table-data)]
    (pp/print-table table)))

(defn get-report [data]
  (println "getting report")
  (let [days  (partition-by #(.getDayOfWeek (:obj (last %)))  data)]
    (print-report days)))
  ;; (get-frequencies data)

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn -main [& args]
  (println "args:" args)
  (let [data (read-file)]
    (if (some #(= "keys" %) args)
      (get-frequencies data))
    (if (some #(= "r" %) args)
      (do (get-report data))
      nil)))

(-main "r")
;; (def job (set-interval get-data 1000))
