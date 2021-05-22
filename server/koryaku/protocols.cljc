(ns koryaku.protocols)

(defprotocol DocumentStore
  (get-seq [this]))

(defprotocol Doc
  (read-frontmatter [this ctx])
  (render-html [this ctx]))