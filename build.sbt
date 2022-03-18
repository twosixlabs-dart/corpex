import sbt._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend( Test )
lazy val WipConfig = config( "wip" ) extend( Test )

lazy val commonSettings = {
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        organization := "com.twosixlabs.dart.corpex",
        scalaVersion := "2.12.7",
        resolvers ++= Seq( "Maven Central" at "https://repo1.maven.org/maven2/",
                           "JCenter" at "https://jcenter.bintray.com",
                           "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" ),
        javacOptions ++= Seq( "-source", "1.8", "-target", "1.8" ),
        scalacOptions += "-target:jvm-1.8",
        useCoursier := false,
        libraryDependencies ++= logging ++
                                scalaTest ++
                                betterFiles ++
                                scalaMock ++
                                dartCommons,
        // `sbt test` should skip tests tagged IntegrationTest
        Test / testOptions := Seq( Tests.Argument( "-l", "com.twosixlabs.dart.test.tags.annotations.IntegrationTest" ) ),
        Test / parallelExecution := false,
        // `sbt integration:test` should run only tests tagged IntegrationTest
        IntegrationConfig / parallelExecution := false,
        IntegrationConfig / testOptions := Seq( Tests.Argument( "-n", "com.twosixlabs.dart.test.tags.annotations.IntegrationTest" ) ),
        // `sbt wip:test` should run only tests tagged WipTest
        WipConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.WipTest" ) ),
    )
}

lazy val disablePublish = Seq(
    skip.in( publish ) := true,
)

lazy val assemblySettings = Seq(
    libraryDependencies ++= scalatra ++ jackson,
    assemblyMergeStrategy in assembly := {
        case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
        case PathList( "reference.conf" ) => MergeStrategy.concat
        case _ => MergeStrategy.last
    },
    test in assembly := {},
    mainClass in( Compile, run ) := Some( "Main" ),
)

sonatypeProfileName := "com.twosixlabs"
inThisBuild(List(
    organization := "com.twosixlabs.dart.corpex",
    homepage := Some(url("https://github.com/twosixlabs-dart/corpex")),
    licenses := List("GNU-Affero-3.0" -> url("https://www.gnu.org/licenses/agpl-3.0.en.html")),
    developers := List(
        Developer(
            "twosixlabs-dart",
            "Two Six Technologies",
            "",
            url("https://github.com/twosixlabs-dart")
        )
    )
))

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"


/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  PROJECT DEFINITIONS                                     ##
   ##                                                                                          ##
   ##############################################################################################
 */

lazy val root = ( project in file( "." ) )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .aggregate( corpexApi, corpexServices, corpexControllers, corpexMicroservice, corpexClient )
  .settings(
      name := "corpex",
      disablePublish
    )

lazy val corpexApi = ( project in file( "corpex-api" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson
                              ++ logging
                              ++ scalaTest
                              ++ jsonValidator
                              ++ betterFiles
                              ++ cdr4s
                              ++ dartCommons,
    )

lazy val corpexServices = ( project in file( "corpex-services" ) )
  .dependsOn( corpexApi )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= database
                              ++ jackson
                              ++ jsonValidator
                              ++ okhttp
                              ++ betterFiles
                              ++ dartRest
                              ++ cdr4s,
      disablePublish,
    )

lazy val corpexControllers = ( project in file( "corpex-controllers" ) )
  .dependsOn( corpexApi, corpexServices )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson
                              ++ scalatra
                              ++ dartRest
                              ++ dartAuth
                              ++ jsonValidator,
   )

lazy val corpexMicroservice = ( project in file( "corpex-microservice" ) )
  .dependsOn( corpexApi, corpexServices, corpexControllers )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .settings(
      commonSettings,
      libraryDependencies ++= database ++ scalatra ++ jackson ++ dartEs,
      assemblySettings,
    )

lazy val corpexClient = ( project in file( "corpex-client" ) )
  .dependsOn( corpexApi )
  .configs( IntegrationConfig, WipConfig )
  .settings(
      commonSettings,
      libraryDependencies ++= betterFiles ++ scalatra ++ jackson,
      assemblySettings,
   )

