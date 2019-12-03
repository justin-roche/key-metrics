(ns key-metrics.scheduling
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.simple :refer [schedule with-repeat-count with-interval-in-milliseconds]]))

(defjob NoOpJob
  [ctx]
  (println "Does nothing"))

(defn set-schedule
  [& m]
  (let [s   (-> (qs/initialize) qs/start)
        job (j/build
             (j/of-type NoOpJob)
             (j/with-identity (j/key "jobs.noop.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (with-repeat-count 10)
                                   (with-interval-in-milliseconds 1000))))]
    (qs/schedule s job trigger)))

(set-schedule)
