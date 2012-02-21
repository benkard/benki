(defproject benki "0.0.0"
  :description "The Benkard Family Hub"
  :dependencies [[org.clojure/clojure "1.3.0"]

                 ;; Clojure Contrib
                 [org.clojure/algo.generic "0.1.0-SNAPSHOT"]
                 [org.clojure/algo.monads "0.1.0"]
                 [org.clojure/core.logic "0.6.5"]
                 [org.clojure/core.match "0.2.0-alpha9"]
                 [org.clojure/core.unify "0.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.finger-tree "0.0.2-SNAPSHOT"]
                 ;;[org.clojure/data.xml.root "0.0.1-SNAPSHOT" :type "pom"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/java.jdbc "0.1.1"]
                 [org.clojure/java.data "0.0.1-SNAPSHOT"]
                 [org.clojure/java.jmx "0.2-SNAPSHOT"]
                 [org.clojure/math.combinatorics "0.0.3-SNAPSHOT"]
                 [org.clojure/math.numeric-tower "0.0.2-SNAPSHOT"]
                 [org.clojure/test.generative "0.1.3"]
                 ;;[org.clojure/test.benchmark "0.1.0-SNAPSHOT"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.macro "0.1.1"]
                 [org.clojure/tools.namespace "0.1.0"]
                 [org.clojure/tools.nrepl "0.0.5"]
                 [org.clojure/tools.trace "0.7.1"]
                 ;;[org.clojure.contrib/standalone "1.3.0-SNAPSHOT"]

                 ;; Web app utilities
                 [ring "1.0.0-RC5"]
                 [noir "1.2.1"]
                 [hiccup "0.3.7"]
                 [cssgen "0.2.5-SNAPSHOT"]
                 ;;[clj-http "0.2.3"]

                 ;; Relational database access
                 [clojureql "1.1.0-SNAPSHOT"]
                 [korma "0.2.1"]
                 [postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]

                 ;; Other databases
                 ;;[com.sleepycat/je "4.1.10"]  ;4.0.96 if the Oracle Maven repo scares you
                 [tokyocabinet "1.24.0"]
                 ;;[com.h2database/h2 "1.3.161"]
                 ;;[hypergraphdb/hypergraphdb "1.1"]     ;no Maven artifact
                 ;;[org.clojars.gregburd/cupboard "1.0.0-SNAPSHOT"]
                 ;;[fleetdb-client "0.2.2"]
                 ;;[uk.co.magus.fourstore/4store-client "1.0"]

                 ;; Additional libraries
                 ;;[clj-oauth2 "0.0.1"]
                 ;;[org.clojars.zef/jopenid "1.05"]
                 [org.openid4java/openid4java-consumer "0.9.6" :type "pom"]
                 [org.jsoup/jsoup "1.6.1"]
                 ]
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

