(ns koryaku.page
  (:require [rum.core :as rum]
            [datascript.core :as d]
            [koryaku.components :as com]))


(rum/defc index-page [{:koryaku.db/keys [conn]
                       :keys [router]
                       :as ctx}]
  (let [blog-list (->> (d/q
                         '[:find [(pull ?e [:koryaku.doc/id :koryaku.doc.meta/title :koryaku.doc.meta/published_at]) ...]
                           :where [?e :koryaku.doc.meta/published_at ?published]]
                         @conn)
                       (sort-by :koryaku.doc.meta/published_at #(compare %2 %1))
                       (group-by #(get-in % [:koryaku.doc.meta/published_at 0])))]
    [:html {:lang "zh-hk"}
     [:head
      [:meta {:charset "utf8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
      [:title "Blog"]
      [:link {:rel "stylesheet" :href "/css/core.css" :type "text/css" :media "all"}]]
     [:body
      [:div.root
       (com/Root ctx)]]]))

(rum/defc post-page [{:koryaku.db/keys [conn] :as ctx}]
  (let [id (get-in ctx [:path-params :id])
        blog (d/q
               '[:find (pull ?e [*]) .
                 :in $ ?id
                 :where [?e :koryaku.doc/id ?id]]
               @conn id)]
    [:html {:lang "zh-hk"
            "prefix" "og: http://ogp.me/ns#"
            "xmlns:og" "http://opengraphprotocol.org/schema/"}
     [:head
      [:title (-> (:koryaku.doc.meta/title blog) first)]
      [:meta {:charset "utf8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
      [:meta {:name "description" :content (-> (:koryaku.doc.meta/description blog) first)}]
      [:link {:rel "stylesheet" :href "/css/core.css" :type "text/css" :media "all"}]]
     [:body
      [:div.root
       (com/Root ctx)]]]))
