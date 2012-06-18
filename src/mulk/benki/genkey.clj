(ns mulk.benki.genkey
  (:refer-clojure)
  (:use [clojure         core repl pprint]
        [hiccup core     page-helpers]
        [mulk.benki      config util db webutil]
        [clojure.core.match
         :only [match]]
        [noir            core]
        [clojure.java.jdbc :only [transaction do-commands]])
  (:require [noir.session      :as session]
            [noir.response     :as response]
            [noir.request      :as request]
            [clojure.java.jdbc :as sql]
            [com.twinql.clojure.http :as http])
  (:import [org.bouncycastle.jcajce.provider.asymmetric.rsa
            BCRSAPublicKey]
           [org.bouncycastle.cert
            X509v3CertificateBuilder
            X509CertificateHolder]
           [org.bouncycastle.operator
            DefaultSignatureAlgorithmIdentifierFinder
            DefaultDigestAlgorithmIdentifierFinder]
           [org.bouncycastle.operator.bc
            BcRSAContentSignerBuilder]
           [org.bouncycastle.asn1.x509
            X509Name
            X509Extension
            SubjectPublicKeyInfo
            GeneralName
            GeneralNames]
           [org.bouncycastle.asn1.x500
            X500Name]))



(defonce signing-keypair
  (-> (doto (org.bouncycastle.crypto.generators.RSAKeyPairGenerator.)
        (.init (org.bouncycastle.crypto.params.RSAKeyGenerationParameters.
                (BigInteger. "123") (java.security.SecureRandom.) 512 123)))
      (.generateKeyPair)))

(defonce cert-signer
  (let [sig-id     (.find (DefaultSignatureAlgorithmIdentifierFinder.) "SHA1withRSA")
        digest-id  (.find (DefaultDigestAlgorithmIdentifierFinder.) sig-id)
        ;;digest-id  (.find (DefaultDigestAlgorithmIdentifierFinder.) "SHA1")
        ]
    (-> (BcRSAContentSignerBuilder. sig-id digest-id)
        (.build (.getPrivate signing-keypair)))))

(defonce cert-serial
  (atom 1N))

(defn join-lines [s]
  (apply str (clojure.string/split s #"\n")))

(defn decode-spkac [spkac]
  (org.bouncycastle.mozilla.SignedPublicKeyAndChallenge.
   (ring.util.codec/base64-decode (join-lines spkac))))

(defmulti spkac-pubkey->map type)

(defmethod spkac-pubkey->map BCRSAPublicKey [pubkey]
  {:modulus  (.getModulus pubkey)
   :exponent (.getPublicExponent pubkey)})

(defn spkac-pubkey [creq]
  (spkac-pubkey->map
   (.getPublicKey creq "BC")))

(defn now []
  (java.util.Date.))

(defn twenty-years-from-now []
  (java.util.Date. (+ (* 20 3600 24 365 1000) (.getTime (now)))))

(defn sign-spkac [spkac user]
  (let [serial     (swap! cert-serial inc)
        pubkeyinfo (-> spkac
                       (.getPublicKeyAndChallenge)
                       (.getSubjectPublicKeyInfo))
        builder    (X509v3CertificateBuilder.
                    (X500Name. "CN=Guenologlyan Mages Association")
                    (java.math.BigInteger. (str serial))
                    (now)
                    (twenty-years-from-now)
                    (X500Name. (fmt nil "CN=Benki User (~a)" (user-nickname user)))
                    pubkeyinfo)]
    (.addExtension builder
                   X509Extension/subjectAlternativeName
                   true
                   (GeneralNames.
                    (GeneralName.
                     GeneralName/uniformResourceIdentifier
                     (link :profile user))))
    (.build builder cert-signer)))

(defpage "/genkey" []
  (redirect "/keys"))

(defpage [:post "/keys/register"] []
  (let [spkac-data (get-in (request/ring-request) [:params :key])
        spkac      (decode-spkac spkac-data)
        pubkey     (spkac-pubkey spkac)]
    (with-dbt
      (query "INSERT INTO rsa_keys (modulus, exponent) VALUES (?::NUMERIC, ?::NUMERIC) RETURNING 't'"
             (str (:modulus pubkey)) (str (:exponent pubkey)))
      (query "INSERT INTO user_rsa_keys (\"user\", modulus, exponent) VALUES (?, ?::NUMERIC, ?::NUMERIC) RETURNING 't'"
             *user* (str (:modulus pubkey)) (str (:exponent pubkey)))
      ;;(redirect (linkrel :keys))
      {:status  200
       :headers {"Content-Type" "application/x-x509-user-cert"}
       :body    (.getEncoded (sign-spkac spkac *user*))})))

(defpage "/keys" []
  (with-auth
    (layout {} "Key Management"
            [:p
             [:h2 "Your Keys"]
             [:table
              {:style "border: 1px solid #000"
               :border "1"}
              [:thead
               [:th {:style "text-align: left"} "Exponent"]
               [:th {:style "text-align: left"} "Modulus"]]
              [:tbody
               (with-dbt
                 (for [{e :exponent, m :modulus}
                       (query "select * from user_rsa_keys where \"user\" = ?" *user*)]
                   [:tr
                    [:td e]
                    [:td m]]))]]]
            [:p
             [:h2 "Generate a Key Pair"]
             [:h3 "RSA"]
             [:form {:method "POST", :action (linkrel :keys :register)}
              [:keygen {:name "key", :keytype "RSA"}]
              [:input {:type "submit" :value "Generate"}]]
             [:h3 "ECDSA"
              [:form {:method "POST", :href (linkrel :keys :register)}
               [:keygen {:name "key", :keytype "EC", :keyparams "secp521r1"}]
               [:input {:type "submit" :value "Generate"}]]]])))
