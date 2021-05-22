(ns koryaku.routes)

(def routes
  [["/" ::index]
   ["/posts/:id/" ::post]
   ["/posts/:id/:res" ::post-resources]
   ["/projects" ::projects]
   ["/consulting" ::consulting]
   ["/about" ::me]])
