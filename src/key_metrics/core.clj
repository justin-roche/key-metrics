(ns key-metrics.db
  (:require [key-metrics.dbpass :as pass]
            [key-metrics.utils :as km-utils]
            [key-metrics.read :as km-read]
            [key-metrics.db :as km-db]
            [key-metrics.report :as km-reports]))

(defn reset []
  (km-db/clear-db)
  (km-read/import-log))

(defn read-new []
  (km-read/import-log)
  (km-db/info))

(km-reports/create-today-report)
