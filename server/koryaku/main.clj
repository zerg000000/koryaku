(ns koryaku.main
  (:require [koryaku.routes :as routes]
            [koryaku.page :as page]
            [koryaku.db :as db]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as reload]
            [ring.util.response :as resp]
            [rum.core :as rum]
            [datascript.core :as d]
            [hawk.core :as hawk]
            [koryaku.commonmark :as cm])
  (:gen-class))


(defn rum-component->handler
  "Create handler which use rum component for rendering"
  [p]
  (fn [req]
    {:status  200
     :headers {"Content-Type" "text/html;charset=utf-8"}
     :body    (str
                "<!doctype html>"
                (rum/render-static-markup (p req)))}))

(defn expand [registry]
  (fn [data opts]
    (if (keyword? data)
      (some-> data
              registry
              (r/expand opts)
              (assoc :name data))
      (r/expand data opts))))


(defn post-resources-handler
  [{:koryaku.db/keys [conn] :as req}]
  (let [{:keys [id res]} (get-in req [:path-params])
        post (d/q '[:find (pull ?e [*]) .
                    :in $ ?id
                    :where [?e :koryaku.doc/id ?id]]
                  @conn id)
        post-dir (-> (:koryaku.doc/uri post) .getParent)
        resource-path (str post-dir "/" res)]
    (resp/file-response resource-path)))


(def pages
  {::routes/index      #'page/index-page
   ::routes/post       #'page/post-page
   ::routes/projects   #(#'page/post-page (assoc-in % [:path-params :id] "projects"))
   ::routes/consulting #(#'page/post-page (assoc-in % [:path-params :id] "consulting"))
   ::routes/me         #'page/index-page})

(def router
  (ring/router routes/routes
               (->> pages
                    (map (fn [[k v]] [k (rum-component->handler v)]))
                    (into {::routes/post-resources post-resources-handler})
                    (expand)
                    (hash-map :expand))))

(defn wrap-router [h routes]
  (let [r (r/router routes)]
    (fn [req]
      (h (assoc req :router r)))))


(defn -main [& args]
  (let [handler (ring/ring-handler
                  router
                  (ring/routes
                    (ring/create-file-handler {:path "/"})
                    (ring/redirect-trailing-slash-handler {:method :add})
                    (ring/create-default-handler)))
        ctx (cm/new-context)
        db (time (db/new-db "content/" ctx))]
    (hawk/watch! [{:paths   ["content/"]
                   :filter  hawk/file?
                   :handler (fn [_ e]
                              (let [doc (db/path->doc (.toPath (:file e)) ctx)]
                                (d/transact! db
                                  (condp = (:kind e)
                                    :create [doc]
                                    :modify [(merge {:db/add [:koryaku.doc/id (:koryaku.doc/id doc)]} doc)]
                                    :delete [:db.fn/retractEntity [:koryaku.doc/id (:koryaku.doc/id doc)]]))))}])
    (jetty/run-jetty
      (-> handler
          (db/wrap-db db)
          (wrap-router routes/routes)
          (reload/wrap-reload))
      {:port 3000})))
