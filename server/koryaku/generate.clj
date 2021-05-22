(ns koryaku.generate
  (:require [babashka.fs :as fs]
            [koryaku.fs :as fs-ext]
            [koryaku.db :as db]
            [datascript.core :as d]
            [reitit.core :as r]
            [koryaku.routes :as routes]
            [koryaku.page :as page]
            [rum.core :as rum]
            [clojure.string :as str]
            [lambdaisland.uri.normalize :as norm]
            [koryaku.commonmark :as cm]))

(defn page->file [f rendered-page]
  (fs-ext/write-string f
                       (str
                         "<!doctype html>"
                         (rum/render-static-markup rendered-page))))


(defn -main [& args]
  (let [ctx (cm/new-context)
        conn (db/new-db "content/" ctx)
        router (r/router routes/routes)]
    ; copy public resources
    (fs/copy-tree
      (fs/path "public/")
      (fs/path "docs/")
      {:replace-existing true})
    ; generate index
    (let [m (r/match-by-name router ::routes/index)
          f (fs/path (str "docs" (:path m) "index.html"))]
      (fs/create-dirs (fs/parent f))
      (page->file f (page/index-page (assoc m ::db/conn conn :uri (:path m) :router router))))
    ; generate pages
    (let [page-ids (d/q '[:find ?id
                          :in $ ?includes?
                          :where
                          [?e :koryaku.doc/id ?id]
                          [?e :koryaku.doc/uri ?uri]
                          [(?includes? ?uri "pages")]]
                        @conn
                        str/includes?)]
      (doseq [[id] page-ids]
        (let [m (r/match-by-name router
                                 (keyword "koryaku.routes" id))
              f (fs/path (str "docs" (norm/percent-decode (:path m))
                              "/" "index.html"))]
          (fs/create-dirs (fs/parent f))
          (page->file f (page/post-page (assoc m ::db/conn conn :uri (:path m) :path-params {:id id} :router router))))))
    ; copy post resources
    (fs/copy-tree
      (fs/path "content/blog/")
      (fs/path "docs/posts/")
      {:replace-existing true})
    ; generate posts
    (let [post-ids (d/q '[:find ?id
                          :in $ ?includes?
                          :where
                          [?e :koryaku.doc/id ?id]
                          [?e :koryaku.doc/uri ?uri]
                          [(?includes? ?uri "blog")]]
                        @conn
                        str/includes?)]
      (doseq [[id] post-ids]
        (let [m (r/match-by-name router
                                 ::routes/post {:id id})
              f (fs/path (str "docs" (norm/percent-decode (:path m)) "index.html"))]
          (fs/create-dirs (fs/parent f))
          (page->file f (page/post-page (assoc m ::db/conn conn :uri (:path m) :router router))))))))