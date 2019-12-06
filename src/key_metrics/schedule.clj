(ns key-metrics.scheduling
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [key-metrics.core :as km-core]
            [key-metrics.read :as km-read]
            [key-metrics.db :as km-db]
            [clojurewerkz.quartzite.schedule.calendar-interval :refer [schedule with-interval-in-seconds]]))

(defjob read-job [ctx]
  (km-read/import-log)
  ;; (km-db/info)
  )

(defn unschedule-reads []
  (let [s   (-> (qs/initialize) qs/start)]
    (qs/delete-job s (j/key "jobs.read.1"))))

(defn schedule-reads []
  (let [s   (-> (qs/initialize) qs/start)
        job (j/build
             (j/of-type read-job)
             (j/with-identity (j/key "jobs.read.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (with-interval-in-seconds 30))))]
    (qs/schedule s job trigger)))

(unschedule-reads)
(schedule-reads)

;; (km-core/read-new)

