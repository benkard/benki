;; -*- mode: clojure -*-

(defproject benki "0.0.0-SNAPSHOT"
  :description "The Benkard Family Hub"
  :dependencies [[org.clojure/clojure "1.4.0"]

                 ;; Clojure Contrib
                 [prxml "1.3.1"]
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
                 [ring "1.1.2"]
                 [noir "1.3.0-beta10"]
                 [hiccup "1.0.0"]
                 [cssgen "0.2.6"]
                 [aleph "0.3.0-alpha3"]
                 [lamina "0.5.0-alpha3"]  ;-alpha4
                 [noir-async "1.1.0-beta9"]

                 ;; Relational database access
                 [clojureql "1.0.3"]
                 [postgresql "9.1-901.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]

                 ;; Additional libraries
                 ;;[clj-oauth2 "0.0.1"]
                 [org.openid4java/openid4java-consumer "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-server   "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-xri      "0.9.6" :extension "pom"]
                 [org.openid4java/openid4java-infocard "0.9.6" :extension "pom"]
                 [xerces "2.4.0"]  ;log4j needs this; OpenID4Java needs log4j
                 [xerces/xercesImpl "2.10.0"]  ;java-rdfa needs this
                 [org.slf4j/slf4j-jcl "1.6.6"]  ;Jena needs this
                 ;;[org.slf4j/slf4j-log4j12 "1.6.6"]  ;doesn't seem to suffice for Jena
                 [org.jsoup/jsoup "1.6.1"]
                 [org.apache.abdera/abdera-parser       "1.1.2"]
                 [org.apache.ws.commons.axiom/axiom-api "1.2.12"]
                 [clj-apache-http "2.3.2"]
                 [org.pegdown/pegdown "1.1.0"]
                 [jivesoftware/smack "3.1.0"]
                 [jivesoftware/smackx "3.1.0"]
                 [joda-time/joda-time "2.1"]
                 ;;[org.bouncycastle/bcmail-jdk15on "1.46"]
                 [org.bouncycastle/bcprov-jdk15on "1.47"]
                 ;;[org.bouncycastle/bcpg-jdk15on "1.46"]
                 [org.bouncycastle/bcpkix-jdk15on "1.47"]
                 ;;[org.bouncycastle/bcprov-ext-jdk15on "1.46"]
                 ;;[org.bouncycastle/bctsp-jdk15on "1.46"]

                 ;; Semantic Web/RDF stuff
                 ;;[net.java.dev.sommer/foafssl "0.3.1" :extension "pom"]
                 ;; [net.java.dev.sommer/foafssl-verifier "0.3.1"]
                 ;; [net.java.dev.sommer/foafssl-filter "0.3.1"]
                 [net.java.dev.sommer/foafssl-verifier "0.5-SNAPSHOT"]
                 [net.java.dev.sommer/foafssl-verifier-sesame "0.5-SNAPSHOT"]
                 ;;[com.hp.hpl.jena/jena "2.6.4"]
                 [net.rootdev/java-rdfa "0.4.2"]
                 [net.rootdev/java-rdfa-htmlparser "0.4.2" :extension "pom"]
                 ;;[nu.validator.htmlparser/htmlparser "1.4"]
                 [org.apache.jena/jena-arq "2.9.1"]
                 [org.apache.jena/jena-core "2.7.1"]
                 [org.apache.jena/jena-tdb "0.9.1"]
                 [org.apache.jena/jena-larq "1.0.0-incubating"]
                 [org.apache.jena/jena-iri "0.9.1"]]
  :plugins [[lein-swank "1.4.4"]
            ;;[lein-immutant "0.8.2"]
            ]
  :exclusions [org.clojure/clojure-contrib  ;you know, the old pre-1.3.0 versions
               org.clojure/clojure          ;so that we can enforce our preferred version
               org.clojure.contrib/prxml]
  ;;:hooks [leiningen.hooks.difftest]
  ;;:warn-on-reflection true     ;breaks M-x clojure-jack-in
  :profiles {:dev    {}
             :tomcat {:plugins [[lein-ring "0.6.6"]]}}
  :repositories {
                 "sonatype-snapshots"
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
                  :snapshots false}
                 "apache-releases"
                 {:url "https://repository.apache.org/content/repositories/releases/"
                  :snapshots false}}
  :source-path "src"
  ;;:jvm-opts ["-Xms32m"]
  :main mulk.benki.main
  :min-lein-version "2.0.0")
