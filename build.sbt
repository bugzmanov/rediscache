lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.4"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.bugzmanov",
      scalaVersion    := "2.13.1"
    )),
    name := "rediscache",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "redis.clients"     % "jedis"                     % "3.2.0",
      "com.google.guava"  % "guava"                     % "28.2-jre",

      "io.dropwizard.metrics5" % "metrics-graphite"     % "5.0.0",
      "io.micrometer"     % "micrometer-registry-graphite"  % "latest.release",
      "fr.davit"          %% "akka-http-metrics-dropwizard" % "1.0.0",

      "it.ozimov"         % "embedded-redis"            % "0.7.2"         % Test,
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )

enablePlugins(JavaAppPackaging)
javaOptions in Universal ++= Seq(
  "-J-XX:MaxRAMPercentage=100",
  "-Dconfig.file=/opt/rediscache/application.conf"
)

enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "expert/docker-java-minimal:jdk12-alpine"
//dockerBaseImage := "java:openjdk-8u77-jre-alpine"

mainClass in Compile := Some("com.bugzmanov.RedisCacheApp")

