(defproject benki "0.0.0"
  :description "The Benkard Family Hub"
  :dependencies [[org.clojure/clojure "1.3.0"]

                 ;; Clojure Contrib
                 [org.clojure/algo.generic "0.1.0"]
                 [org.clojure/algo.monads "0.1.0"]
                 [org.clojure/core.logic "0.6.5"]
                 [org.clojure/core.match "0.2.0-alpha9"]
                 [org.clojure/core.unify "0.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/java.jdbc "0.1.1"]
                 [org.clojure/test.generative "0.1.3"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.macro "0.1.1"]
                 [org.clojure/tools.namespace "0.1.0"]
                 [org.clojure/tools.nrepl "0.0.5"]
                 [org.clojure/tools.trace "0.7.1"]

                 ;; Web app utilities
                 [ring "1.0.2"]
                 [noir "1.2.1"]
                 [hiccup "0.3.7"]
                 [cssgen "0.2.5"]

                 ;; Relational database access
                 [clojureql "1.0.3"]
                 [korma "0.2.1"]
                 [postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]

                 ;; Additional libraries
                 ;;[clj-oauth2 "0.0.1"]
                 [org.openid4java/openid4java-consumer "0.9.6" :type "pom"]
                 [org.jsoup/jsoup "1.6.1"]
                 [org.apache.abdera/abdera-parser "1.1.1"]
                 ]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
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
                 ;;"clojars"
                 ;;{:url "http://clojars.org/repo/"
                 ;; :snapshots true
                 ;;},
                 "oracle"
                 {:url "http://download.oracle.com/maven/"
                  :snapshots false}}
  :source-path "src"
  ;;:jvm-opts ["-Xms32m"]
  :main mulk.benki.main)

