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

(defn epoch-to-hhmm [epoch]
  (.format (java.time.LocalDateTime/ofInstant
            (java.time.Instant/ofEpochSecond epoch)
            (java.time.ZoneId/systemDefault)) (get-formatter "hh:mm")))

(defn epoch-to-ldt [epoch]
  (java.time.LocalDateTime/ofInstant
   (java.time.Instant/ofEpochSecond epoch)
   (java.time.ZoneId/systemDefault)))

(defn epoch-to-clock-hour [epoch]
  (.getHour (java.time.LocalDateTime/ofInstant
             (java.time.Instant/ofEpochSecond epoch)
             (java.time.ZoneId/systemDefault))))

(defn format-by-clock-time [ldt]
  (.format ldt (get-formatter "hh:mm")))

(defn parse-date [d]
  (let [formatter  (get-formatter "E MMM dd HH:mm:ss yyyy")
        ldt  (java.time.LocalDateTime/parse d formatter)]
    {:obj ldt
     :epoch (get-epoch ldt)
     :hour (.getHour ldt)}))

(defn second-to-hours [s]
  (/ (/ s 60) 60))

(defn get-formatter [s]
  (java.time.format.DateTimeFormatter/ofPattern s))
(def date-read-formatter (get-formatter date-read-format))

(defn raw-date-to-epoch [d]
  (.toEpochSecond (.atZone  (java.time.LocalDateTime/parse d date-read-formatter)
                            (java.time.ZoneId/systemDefault))))

(defn epoch-to-record-date [d]
  ;; convert epoch seconds to dd-mm-yyyy format
  (.format (java.time.LocalDateTime/ofInstant
            (java.time.Instant/ofEpochSecond d)
            (java.time.ZoneId/systemDefault))
           (get-formatter date-save-format)))

(defn ldt-to-record-date [ldt]
  (epoch-to-record-date (get-epoch ldt)))

(defn get-todays-record-date []
  (epoch-to-record-date (get-epoch (java.time.LocalDateTime/now))))
