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
                 [org.clojure/data.xml "0.0.3"]
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
                 [aleph "0.2.1-beta2"]
                 [noir-async "1.0.0-SNAPSHOT"]

                 ;; Relational database access
                 [clojureql "1.0.3"]
                 [korma "0.2.1"]
                 [postgresql "9.0-801.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]

                 ;; Additional libraries
                 ;;[clj-oauth2 "0.0.1"]
                 [org.openid4java/openid4java-consumer "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-server   "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-xri      "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-infocard "0.9.6" :extension "pom"]
                 [org.jsoup/jsoup "1.6.1"]
                 [org.apache.abdera/abdera-parser       "1.1.2"]
                 [org.apache.ws.commons.axiom/axiom-api "1.2.12"]
                 [clj-apache-http "2.3.2"]
                 [org.pegdown/pegdown "1.1.0"]
                 [jivesoftware/smack "3.1.0"]
                 [jivesoftware/smackx "3.1.0"]
                 ]
  :plugins [[lein-swank "1.4.3"]]
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
                  :snapshots false}
                 "scala-releases"  ;pegdown
                 {:url "http://scala-tools.org/repo-releases"
                  :snapshots false}}
  :source-path "src"
  ;;:jvm-opts ["-Xms32m"]
  :main mulk.benki.main
  :min-lein-version "2.0.0")

