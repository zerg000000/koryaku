(ns koryaku.commonmark
  (:require [koryaku.protocols :as p])
  (:import (org.commonmark.node Document)
           (org.commonmark.ext.front.matter YamlFrontMatterVisitor YamlFrontMatterExtension)
           (java.nio.file Files Path)
           (org.commonmark.parser Parser)
           (org.commonmark.renderer.html HtmlRenderer)))

(defn get-frontmatter [^Document doc]
  (let [visitor (YamlFrontMatterVisitor.)]
    (.accept doc visitor)
    (.getData visitor)))

(defn new-context []
  (let [exts [(YamlFrontMatterExtension/create)]]
    {:parser   (-> (Parser/builder)
                   (.extensions exts)
                   (.build))
     :renderer (-> (HtmlRenderer/builder)
                   (.extensions exts)
                   (.build))}))

(extend-type Path
  p/Doc
  (read-frontmatter [this {:keys [parser]}]
    (let [doc (.parseReader ^Parser parser (Files/newBufferedReader this))]
      (get-frontmatter doc)))
  (render-html [this {:keys [parser renderer]}]
    (with-open [rdr (Files/newBufferedReader this)]
      (.render renderer (.parseReader parser rdr)))))
