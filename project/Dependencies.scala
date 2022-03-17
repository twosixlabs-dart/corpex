import sbt._

object Dependencies {

    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.3"
    val scalaTestVersion = "3.0.5"
    val scalatraVersion = "2.7.1"
    val jettyWebappVersion = "9.4.18.v20190429"
    val servletApiVersion = "3.1.0"
    val betterFilesVersion = "3.8.0"
    val jacksonVersion = "2.9.9"
    val okhttpVersion = "4.1.0"
    val scalaMockVersion = "4.1.0"
    val cdr4sApisVersion = "2.0.74"
    val cdr4sVersion = "3.0.256"
    val corpexApiVersion = "3.0.166"
    val slickVersion = "3.3.2"
    val c3p0Version = "0.9.5.1"
    val postgresVersion = "42.0.0"
    val slickPgVersion = "0.18.1"
    val cdrAggregatorVersion = "3.0-SNAPSHOT"
    val dartCommonsVersion = "3.0.285"
    val dartRestVersion = "3.0.15"
    val dartAuthVersion = "3.0.100"
    val dartEsVersion = "3.0-SNAPSHOT"
    val tapirVersion = "0.17.13"
    val circeVersion = "0.13.0"


    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test",
                        "org.eclipse.jetty" % "jetty-webapp" % jettyWebappVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion )

    val scalaTest = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % "test" )

    val jackson = Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                       "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion )

    val jsonValidator = Seq( "org.everit.json" % "org.everit.json.schema" % "1.5.1" )

    val okhttp = Seq( "com.squareup.okhttp3" % "okhttp" % okhttpVersion,
                      "com.squareup.okhttp3" % "mockwebserver" % okhttpVersion )

    val scalaMock = Seq( "org.scalamock" %% "scalamock" % scalaMockVersion )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-cli" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion )

    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-core" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

    val cdr4sApis = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-apis" % cdr4sApisVersion )

    val corpexApi = Seq( "com.twosixlabs.dart" %% "corpex-api" % corpexApiVersion )

    val database = Seq( "com.typesafe.slick" %% "slick" % slickVersion,
                        "org.postgresql" % "postgresql" % postgresVersion,
                        "com.github.tminglei" %% "slick-pg" % slickPgVersion,
                        "com.mchange" % "c3p0" % c3p0Version )

    val cdrAggregator = Seq( "com.twosixlabs.dart" %% "cdr-aggregator" % cdrAggregatorVersion )

    val dartAuth = Seq( "com.twosixlabs.dart" %% "dart-auth-commons" % dartAuthVersion )

    val dartRest = Seq( "com.twosixlabs.dart.rest" %% "dart-scalatra-commons" % dartRestVersion )

    val dartEs = Seq( "com.twosixlabs.dart.elasticsearch" %% "es-tenant-index" % dartEsVersion,
                      "com.twosixlabs.dart.elasticsearch" %% "es-util" % dartEsVersion )

}
