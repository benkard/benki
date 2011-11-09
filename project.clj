(defproject benki "0.0.0"
  :description "The Benkard Family Hub"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure.contrib/standalone "1.3.0-SNAPSHOT"]
                 [org.clojure/core.match "0.2.0-SNAPSHOT"]
                 [org.clojure/core.logic "0.6.6-SNAPSHOT"]
                 [org.clojure/core.unify "0.5.2-SNAPSHOT"]
                 [org.clojure/data.finger-tree "0.0.2-SNAPSHOT"]

                 ;; Web app utilities
                 [ring "1.0.0-RC1"]
                 [noir "1.2.0"]
                 [hiccup "0.3.7"]
                 [cssgen "0.2.5-SNAPSHOT"]
                 ;;[clj-http "0.2.3"]

                 ;; Relational database access
                 [clojureql "1.1.0-SNAPSHOT"]
                 [korma "0.2.1"]
                 [postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]

                 ;; Other databases
                 [com.sleepycat/je "4.0.92"]
                 ;;[com.h2database/h2 "1.3.161"]
                 ;;[hypergraphdb/hypergraphdb "1.1"]     ;no Maven artifact
                 ;;[org.clojars.gregburd/cupboard "1.0.0-SNAPSHOT"]
                 ;;[fleetdb-client "0.2.2"]
                 ;;[uk.co.magus.fourstore/4store-client "1.0"]

                 ;; Additional libraries
                 ;;[clj-oauth2 "0.0.1"]
                 ;;[org.clojars.zef/jopenid "1.05"]
                 [org.openid4java/openid4java-consumer "0.9.6" :type "pom"]]
  :dev-dependencies [;;[swank-clojure "1.3.2"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     ;;[ring-serve "0.1.1"]
                     [clj-stacktrace "0.2.3"]]
  :exclusions [org.clojure/clojure-contrib]  ;you know, the old pre-1.3.0 versions
  ;;:hooks [leiningen.hooks.difftest]
  ;;:warn-on-reflection true     ;breaks M-x clojure-jack-in
  :repositories {"sonatype-snapshots"
                 ;;https confuses leiningen (but not cake)
                 {:url "http://oss.sonatype.org/content/repositories/snapshots/"
                  :snapshots true},
                 "sonatype-releases"
                 ;;https confuses leiningen (but not cake)
                 {:url "http://oss.sonatype.org/content/repositories/releases/"
                  :snapshots false},
                 "openid4java-releases"
                 {:url "http://oss.sonatype.org/content/repositories/openid4java-releases/"
                  :snapshots false},
                 "clojars" "http://clojars.org/repo/"}
  :source-path "src"
  ;;:jvm-opts ["-Xms32m"]
  :main mulk.benki.main)

