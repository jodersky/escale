lazy val escale = project
  .in(file("."))
  .settings(
    scalaVersion := "2.12.7",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-async" % "0.9.7"
    )
  )

lazy val example = project
  .in(file("example"))
  .dependsOn(escale)
  .settings(
    scalaVersion := "2.12.7"
  )
