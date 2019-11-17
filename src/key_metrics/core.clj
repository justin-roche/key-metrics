(ns key-metrics.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(def log-path "/Users/justin/logfile.txt")
(def log-file-delimiter "::")
(def keys-per-hour 4000)

(def desk-key-interval 100)
;; how close (in seconds) should two keys be for you to be considered "at your desk"

(def typing-key-interval 5)
;; how close (in seconds) should two keys be for you to be considered typing

(def keys-per-day (* keys-per-hour 8))

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))

(defn get-epoch [ldt]
  (.toEpochSecond (.atZone  ldt (java.time.ZoneId/systemDefault))))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :epoch (get-epoch ldt)
     :hour (.getHour ldt)}))

(defn add-line [l]
  ;; parse a single line of the log file; parse individual keys to remove surrounding elements
  (let [entry  (->> (re-pattern log-file-delimiter)
                    (str/split l)
                    (map str/trim))]
    {:key (str/replace (first entry) #"\[|\]" "")
     :time (parse-date (last entry))}))

(defn read-file []
  (with-open [rdr (clojure.java.io/reader log-path)]
    (doall (map add-line (filter #(> (count %) 0) (line-seq rdr))))))

(defn part-hour [keys]
  (partition-by #(:hour (:time %)) keys))

(defn hourly-report [hour-collection]
  {:hour (:hour (:time (last hour-collection)))
   :count (count hour-collection)})

(defn get-epoch-difference [a b]
  (- (:epoch (:time a))
     (:epoch (:time b))))

(defn second-to-hours [s]
  (/ (/ s 60) 60))

(defn sum-valid-keys [keys interval]
;; accumulate the interval difference as a running total if the difference is less than the specified interval 
  (second-to-hours (loop [i 1 c 0]
                     (if (= i (count keys))
                       c
                       (let [a (nth keys i)
                             b (nth keys (dec i))
                             dif (get-epoch-difference a b)
                             p (< dif interval)]
                         (recur (inc i) (+ c (if p dif 0))))))))

(defn get-key-hours [day]
  ;; get the number of work hours per day based on estimated keys per work hour
  (int (/ (count day) keys-per-hour)))

(defn get-percent-for-day [d]
  (int (* 100 (/ (count d) keys-per-day))))

(defn print-today-report [days]
  (let [today (vec (last days))
        part (part-hour today)
        table [{:name  "keys " :value (str (count today) "/" keys-per-day)}
               {:name  "percent keys " :value (get-percent-for-day today)}
               {:name  "key hours " :value (get-key-hours today)}
               {:name  "typing hours " :value (format "%.2f" (double (sum-valid-keys today typing-key-interval)))}
               {:name  "desk hours " :value (format "%.2f" (double (sum-valid-keys today desk-key-interval)))}
               {:name  "keys this hour" :value (count (last part))}]]
    (pp/print-table table)))

(defn get-frequencies [data]
  (let [table-data (->> (map first data)
                        frequencies
                        (sort-by last)
                        vec)
        table (map  (fn [el]
                      {:key (first el)
                       :count (second el)}) table-data)]
    (pp/print-table table)))

(defn get-report [data]
  (let [days  (partition-by #(.getDayOfWeek (:obj (:time %)))  data)]
    (print-today-report days)))

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
