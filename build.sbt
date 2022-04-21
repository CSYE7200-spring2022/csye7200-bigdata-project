
name := "csye7200_project"
version := "1.0"
scalaVersion := "2.12.12"

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-deprecation", "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused" )

val sparkVersion = "3.1.1"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """bigDataProject""",

    // exclude slf4j in each library
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-mllib" % sparkVersion,
      "io.jhdf" % "jhdf" % "0.6.6"
    ).map(_.exclude("org.slf4j", "*")),

    // ensure only one slf4j library
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.11"
    )
  )