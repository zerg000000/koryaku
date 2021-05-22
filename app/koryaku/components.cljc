(ns koryaku.components
  (:require [reitit.core :as r]
            [rum.core :as rum]
            [clojure.string :as str]))

(defn match? [router ks uri]
  (let [m (r/match-by-path router uri)]
    (ks (get-in m [:data :name]))))

(rum/defc BlogHeader [{:keys [uri router]}]
    [:section.blog-header
     [:div.blog-title "It's all about Data"]
     [:ul.blog-header_menu
      (for [[text link ks] [["Blog" ::routes/index #{::routes/post ::routes/index}]
                            ["Projects" ::routes/projects #{::routes/projects}]
                            ["Consulting" ::routes/consulting #{::routes/consulting}]
                            ["About" ::routes/me #{::routes/me}]]]
        [:li.blog-header_menu-item
         (when (match? router ks uri)
           {:class "current"})
         [:a {:href (:path (r/match-by-name router link))} text]])]])

(rum/defc blog-list-per-year [year posts router]
  [:<>
   [:h3.blog-list-year (str year)]
   [:ul.blog-list
    (for [{:koryaku.doc.meta/keys [title published_at]
           :koryaku.doc/keys [id]} posts]
      [:li.blog-list-item
       [:a {:href (:path (r/match-by-name router ::routes/post {:id id}))}
        (first title)]
       [:span (str (nth published_at 2) "/" (nth published_at 1))]])]])

(rum/defc BlogList [blog-list router]
  [:<>
   (for [[year posts] blog-list]
    (blog-list-per-year year posts router))])

(rum/defc BlogPost [blog]
  [:<>
    [:section.post-header
     [:h1 (first (:koryaku.doc.meta/title blog))]
     [:span.published-date (str/join "/" (take 3 (:koryaku.doc.meta/published_at blog)))]]
    [:section.post-content
     {:dangerouslySetInnerHTML {:__html ((:koryaku.doc/markdown blog))}}]])

(rum/defc Root [ctx]
  [:<>
   (BlogHeader ctx)
   (condp =
     :blog-list (BlogList [] {})
     :blog-post (BlogPost {}))])

