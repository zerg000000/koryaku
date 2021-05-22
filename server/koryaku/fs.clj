(ns koryaku.fs
  (:require [koryaku.protocols :as p])
  (:import (java.nio.file Path Files FileVisitOption OpenOption)))

(defn write-string [^Path p ^CharSequence cs & args]
  (Files/writeString p cs (into-array OpenOption args)))

(extend-protocol p/DocumentStore
  Path
  (get-seq [this]
    (-> (Files/walk this (into-array FileVisitOption []))
        (.iterator)
        (iterator-seq))))