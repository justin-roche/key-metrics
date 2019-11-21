(ns key-metrics.utils)

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))

(def date-read-format "E MMM dd HH:mm:ss yyyy")
;; the format for dates in the raw keylog

(def date-save-format "dd-MM-YYYY")
;; the format to store dates in the db

(def date-read-formatter (get-formatter date-read-format))
(def date-save-formatter (get-formatter date-save-format))

(defn get-epoch [ldt]
  (.toEpochSecond (.atZone  ldt (java.time.ZoneId/systemDefault))))

(defn format-by-clock-time [ldt]
  (.format ldt (get-formatter "hh:mm")))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :epoch (get-epoch ldt)
     :hour (.getHour ldt)}))

(defn get-epoch-difference [a b]
  (- (:epoch (:time a))
     (:epoch (:time b))))

(defn second-to-hours [s]
  (/ (/ s 60) 60))
