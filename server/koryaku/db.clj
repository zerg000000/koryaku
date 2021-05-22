(ns koryaku.db
  (:require [clojure.instant :as inst]
            [datascript.core :as d]
            [babashka.fs :as fs]
            [koryaku.fs]
            [koryaku.commonmark]
            [koryaku.protocols :as p])
  (:import (java.nio.file Path)))

(defn data->meta [base m]
  (reduce-kv
    (fn [acc k v]
      (assoc acc
        (keyword "koryaku.doc.meta" k)
        (condp = k
          "published_at" (inst/parse-timestamp vector (first v))
          v)))
    base
    (into {} m)))


(defn path->doc [^Path f ctx]
  (-> {:koryaku.doc/markdown #(p/render-html f ctx)
       :koryaku.doc/uri      f
       :koryaku.doc/id       (-> f (.getParent) (.getFileName) (.toString))
       :koryaku.doc/source   :koryaku.doc.source/java-path}
      (data->meta (p/read-frontmatter f ctx))))

(defn new-db [path ctx]
  (let [xf (comp (filter #(-> % (.toString) (.endsWith ".md")))
                 (map #(path->doc % ctx)))
        files (into [] xf (p/get-seq (fs/path path)))
        conn (d/create-conn)]
    (d/transact! conn files)
    conn))


(defn wrap-db [h db]
  (let [conn db]
    (fn [req]
      (h (assoc req :koryaku.db/conn conn)))))