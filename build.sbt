// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

lazy val escale = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-async" % "0.9.7",
      "com.lihaoyi" %%% "utest" % "0.6.6" % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalaVersion := crossScalaVersions.value.head
  )
  .jsSettings(
    crossScalaVersions := "2.12.6" :: "2.11.12" :: Nil
  )
  .jvmSettings(
    crossScalaVersions := "2.12.7" :: "2.11.12" :: Nil
  )
  .nativeSettings(
    crossScalaVersions := "2.11.12" :: Nil,
    nativeLinkStubs := true
  )


lazy val example = project
  .in(file("example"))
  .dependsOn(escale.jvm)
  .settings(
    scalaVersion := "2.12.7"
  )
