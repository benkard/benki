(defproject benki "0.0.0"
  :description "The Benkard Family Hub"
  :dependencies [;;[org.clojure/clojure "1.4.0-master-SNAPSHOT"]
                 [org.clojure/clojure "1.3.0"]
                 ;;[org.clojure.contrib/standalone "1.3.0-alpha4"]
                 [org.clojure.contrib/standalone "1.3.0-SNAPSHOT"]
                 ;;[match "0.2.0-SNAPSHOT"]
                 ;; https://oss.sonatype.org/content/repositories/snapshots/org/clojure/
                 [org.clojure/core.match "0.2.0-SNAPSHOT"]
                 [org.clojure/core.logic "0.6.6-SNAPSHOT"]
                 [org.clojure/core.unify "0.5.2-SNAPSHOT"]
                 [org.clojure/data.finger-tree "0.0.2-SNAPSHOT"]
                 [ring "1.0.0-RC1"]
                 [noir "1.2.0"]
                 [korma "0.2.0"]
                 ;;[clj-http "0.2.3"]
                 [hiccup "0.3.7"]
                 [cssgen "0.2.5-SNAPSHOT"]

                 [clojureql "1.1.0-SNAPSHOT"]
                 
                 ;;[useful "0.7.4-alpha4"]
                 ;; [trafficdb "0.5.0-SNAPSHOT"]
                 ;; [org.clojars.zakwilson/plasma "0.3.0-SNAPSHOT"]
                 ;; ;;[plaza "0.0.5-SNAPSHOT"]
                 ;; [plaza-fork "0.0.5.2-SNAPSHOT"]
                 ;; [clj-oauth2 "0.0.1"]
                 ;; [org.clojars.zef/jopenid "1.05"]
                 ;; ;;[uk.co.magus.fourstore/4store-client "1.0"]
                 ]
  :dev-dependencies [;;[swank-clojure "1.3.2"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     ;;[ring-serve "0.1.1"]
                     [clj-stacktrace "0.2.3"]]
  :repositories [["sonatype-snapshots"
                  ;;https confuses leiningen (but not cake)
                  "http://oss.sonatype.org/content/repositories/snapshots/"]
                 ["clojars" "http://clojars.org/repo/"]])

