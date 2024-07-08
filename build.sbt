import sbtassembly.MergeStrategy.defaultMergeStrategy

import scala.collection.Seq

lazy val root = (project in file("."))
  .settings(
    name := "S3HyperSync",
    organization := "io.github.starofall",
    version := "0.1.5",
    scalaVersion := "2.13.14",
    assembly / assemblyMergeStrategy := {
      case x if x.contains("META-INF") && x.contains("module-info.class") => MergeStrategy.discard
      case x if x.contains("META-INF") && x.contains("license")           => MergeStrategy.first
      case "application.conf"                                             => MergeStrategy.concat
      case "logback.xml"                                                  => MergeStrategy.preferProject
      case sbtassembly.PathList("module-info.java")                       => MergeStrategy.discard
      case sbtassembly.PathList("module-info.class")                      => MergeStrategy.discard
      case x                                                              => defaultMergeStrategy(x)
    },
    assembly / test := {},
    fork := true,
    scalacOptions ++= Seq(
      "-Xasync",
      "-deprecation", // Emit warning and location for usages of deprecated APIs
      "-unchecked", // Enable additional warnings where generated code depends on assumptions
      "-Xfatal-warnings", // Fail the compilation if there are any warnings
      "-feature",
      "-Ybackend-parallelism", "8",
      "-Ybackend-worker-queue", "8",
      "-encoding", "utf8"),
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "5.1.0",
      "org.apache.pekko" %% "pekko-connectors-s3" % "1.0.2",
      "org.apache.pekko" %% "pekko-stream" % "1.0.2",
      "ch.qos.logback" % "logback-core" % "1.4.14",
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      // testing dependencies
      "org.scalatest" %% "scalatest" % "3.2.19" % "test",
      "io.github.robothy" % "local-s3-rest" % "1.15" % "test"
      )
    )
