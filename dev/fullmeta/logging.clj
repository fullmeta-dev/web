(ns fullmeta.logging
  (:require
   [clojure.tools.logging.impl]
   [clojure.tools.logging :as log]
   [clojure.xml :as xml]
   [clojure.java.io :as io]))

;;* Logback

(def logback-dev-config
  [:configuration {:scanPeriod "5 seconds" :scan "true"}

   [:appender {:class "ch.qos.logback.core.ConsoleAppender" :name "STDOUT"}
    [:filter {:class "ch.qos.logback.classic.filter.ThresholdFilter"}
     [:level "DEBUG"]]
    [:encoder
     [:pattern "%date %highlight(%-5level) %logger{36}: %msg%n"]
     [:charset "UTF-8"]]]

   [:appender {:class "ch.qos.logback.core.rolling.RollingFileAppender" :name "all"}
    [:filter {:class "ch.qos.logback.classic.filter.ThresholdFilter"}
     [:level "INFO"]]
    [:encoder [:pattern "%date %-5level %logger{25}: %msg %X %n"]]
    [:file "log/all.log"]
    [:rollingPolicy
     {:class "ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy"}
     [:fileNamePattern "log/all.%d{yyyy-MM-dd}.%i.log"]
     [:maxFileSize "20 MB"]
     [:maxHistory "15"]
     [:totalSizeCap "512MB"]
     [:cleanHistoryOnStart "true"]]]

   [:appender {:class "ch.qos.logback.core.rolling.RollingFileAppender" :name "app"}
    [:filter {:class "ch.qos.logback.classic.filter.ThresholdFilter"}
     [:level "DEBUG"]]
    [:encoder [:pattern "%date %-5level %logger{25}: %msg %X %n"]]
    [:file "log/fullmeta.log"]
    [:rollingPolicy
     {:class "ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy"}
     [:fileNamePattern "log/fullmeta.%d{yyyy-MM-dd}.%i.log"]
     [:maxFileSize "20 MB"]
     [:maxHistory "15"]
     [:totalSizeCap "512MB"]
     [:cleanHistoryOnStart "true"]]]

   [:root {:level "ALL"}
    [:appender-ref {:ref "all"}]]

   [:logger {:name "fullmeta"}
    [:appender-ref {:ref "STDOUT"}]
    [:appender-ref {:ref "app"}]]

   [:contextListener {:class "ch.qos.logback.classic.jul.LevelChangePropagator"}
    [:resetJUL "true"]]])

(def logback-prod-config
  [:configuration
   [:appender {:class "ch.qos.logback.core.ConsoleAppender" :name "STDOUT"}
    [:encoder [:pattern "%date [%thread] %-5level %logger{36} - %msg%n"]]]
   [:root {:level "info"}
    [:appender-ref {:ref "STDOUT"}]]])

;;* Log4j2

(def log4j2-dev-config
  ;; https://logging.apache.org/log4j/2.x/manual/configuration.html
  [:Configuration {:monitorInterval "5" :status "WARN"}
   [:Appenders

    ;; Console
    [:Console {:target "SYSTEM_OUT" :name "console"}
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]]

    ;; All INFO
    [:RollingFile {:name "all" :bufferedIO "true" :fileName "logs/all.log" :filePattern "logs/all.%i.log"}
     [:ThresholdFilter {:level "INFO" :onMatch "ACCEPT" :onMismatch "DENY"}]
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]
     [:Policies
      [:SizeBasedTriggeringPolicy {:size "20MB"}]]
     [:DefaultRolloverStrategy {:max "5"}]]

    ;; App INFO
    [:RollingFile {:name "app" :bufferedIO "true" :fileName "logs/app.log" :filePattern "logs/app.%i.log"}
     [:ThresholdFilter {:level "INFO" :onMatch "ACCEPT" :onMismatch "DENY"}]
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]
     [:Policies
      [:SizeBasedTriggeringPolicy {:size "20MB"}]]
     [:DefaultRolloverStrategy {:max "5"}]]

    ;; App DEBUG
    [:RollingFile {:name "debug" :bufferedIO "true" :fileName "logs/debug.log" :filePattern "logs/debug.%i.log"}
     [:ThresholdFilter {:level "DEBUG" :onMatch "ACCEPT" :onMismatch "DENY"}]
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]
     [:Policies
      [:SizeBasedTriggeringPolicy {:size "20MB"}]]
     [:DefaultRolloverStrategy {:max "5"}]]]

   [:Loggers

    [:Root {:level "ALL"}
     [:AppenderRef {:ref "all"}]]

    [:Logger {:name "fullmeta" :level "debug"}
     [:AppenderRef {:ref "app"}]
     [:AppenderRef {:ref "console"}]
     [:AppenderRef {:ref "debug"}]]

    [:Logger {:name "user" :level "debug" :additivity "false"}
     [:AppenderRef {:ref "console"}]
     [:AppenderRef {:ref "debug"}]]]])

(def log4j2-prod-config
  ;; https://logging.apache.org/log4j/2.x/manual/configuration.html
  [:Configuration {:monitorInterval "5" :status "WARN"}
   [:Appenders

    [:Console {:target "SYSTEM_OUT" :name "console"}
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]]

    [:RollingFile {:name "app" :bufferedIO "true" :fileName "/var/log/fullmeta.log" :filePattern "/var/log/fullmeta.%i.log"}
     [:ThresholdFilter {:level "INFO" :onMatch "ACCEPT" :onMismatch "DENY"}]
     [:PatternLayout {:pattern "%date %-5level [%logger] %message%n%throwable"}]
     [:Policies
      [:SizeBasedTriggeringPolicy {:size "20MB"}]]
     [:DefaultRolloverStrategy {:max "5"}]]]

   [:Loggers
    [:Root {:level "INFO"}]
    [:Logger {:name "fullmeta" :level "INFO"}
     [:AppenderRef {:ref "app"}]
     [:AppenderRef {:ref "console"}]]]])

(comment
  ;; TODO(vlad) Either figure out how to get log4j2 working or give up and remove entirely

  ;; If we ever attempt to switch
  ;; :jvm-opts
  ;;   "-Dlog4j2.configurationFile=log4j2.xml"
  ;;   "-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory"
  ;;
  ;; :dependencies
  ;;   [org.apache.logging.log4j/log4j-api "2.18.0"]
  ;;   [org.apache.logging.log4j/log4j-core "2.18.0"]
  ;;   [org.apache.logging.log4j/log4j-1.2-api "2.18.0"]
  ;;   [org.apache.logging.log4j/log4j-jcl "2.18.0"]
  ;;   [org.apache.logging.log4j/log4j-jul "2.18.0"]
  ;;   [org.apache.logging.log4j/log4j-slf4j-impl "2.18.0"]

  (defn start-logging! []
    ;; When backend set in properties requiring clojure.tools.logging automatically loads it, so this
    ;; case is a noop and property settings should be honored.
    (when-not (System/getProperty "clojure.tools.logging.factory")
      ;; Honor config set in properties
      (when-not (System/getProperty "log4j2.configurationFile")
        (println "Starting logger ...")
        (System/setProperty "log4j2.configurationFile" "log4j2.xml"))
      (alter-var-root
       (var log/*logger-factory*)
       (constantly (clojure.tools.logging.impl/log4j2-factory))))
    (log/info (str "Logging with: " (.name log/*logger-factory*))))

  ;; NOTE(vlad) This procedure shouldn't be used under normal circumstances - prefer relying on
  ;; logging backend re-scanning its config files: log4j2 offers monitorInterval and logback scan +
  ;; scanPeriod. Heads up if you use Emacs CIDER restarting logger like this won't effect your
  ;; development session but it appears to effect system processes so that when you kill your repl
  ;; it'll kill the entire Emacs process.
  (defn reconfigure-logger []
    ;; stop
    (-> log/*logger-factory*
        (clojure.tools.logging.impl/get-logger *ns*)
        (.getContext)
        (.getConfiguration)
        (.stop))
    ;; reconfigure
    (-> log/*logger-factory*
        (clojure.tools.logging.impl/get-logger *ns*)
        (.getContext)
        (.reconfigure)))
  ;; comment
  )

;;* Helpers

;; Poorman's hiccup<->enlive transformers: not tail-recursive so don't blow the stack - only use on
;; small blobs. Anything beyond our logger config try tupelo.forest from tupelo lib.
(defn enlive [node]
  (if (vector? node)
    (let [[tag attrs & content] node]
      {:attrs (if (map? attrs) attrs {})
       :tag tag
       :content (or (mapv enlive (if (map? attrs) content (when attrs (into [attrs] content))))
                    [])})
    node))

(defn hiccup [{:keys [tag attrs content] :as node}]
  (if (string? node)
    node
    (into
     (if (empty? attrs) [tag] [tag attrs])
     (map hiccup content))))

(defn read-xml-config [resource-path-to-xml]
  (-> resource-path-to-xml
      io/resource
      io/as-file
      xml/parse
      hiccup))

(defn write-xml-config
  ([hiccup]
   (-> hiccup enlive xml/emit))
  ([path-to-xml hiccup]
   (spit
    path-to-xml
    (with-out-str
      (write-xml-config hiccup)))))

(comment

  ;; logback
  (read-xml-config "logback.xml")
  (write-xml-config "env/dev/resources/logback.xml" logback-dev-config)
  (write-xml-config "env/prod/resources/logback.xml" logback-prod-config)

  ;; log4j2
  (read-xml-config "log4j2.xml")
  (write-xml-config "env/dev/resources/log4j2.xml" log4j2-dev-config)
  (write-xml-config "env/prod/resources/log4j2.xml" log4j2-prod-config)

  ;; comment
  )
