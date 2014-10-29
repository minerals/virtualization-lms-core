name := "LMS"

version := "0.3-SNAPSHOT"

organization := "EPFL"

scalaOrganization := "org.scala-lang"

scalaVersion := virtScala

scalaHome := Some(file(sys.env("SCALA_VIRTUALIZED_HOME")))

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "test-src")

scalacOptions += "-Yvirtualize"

//scalacOptions += "-Yvirtpatmat"

//scalacOptions in Compile ++= Seq(/*Unchecked, */Deprecation)


libraryDependencies += "org.scala-lang" % "scala-library" % virtScala

libraryDependencies += "org.scala-lang" % "scala-compiler" % virtScala

libraryDependencies += "org.scala-lang.plugins" % "scala-continuations-library_2.11" % "1.0.2"

libraryDependencies += scalaTest


// tests are not thread safe
parallelExecution in Test := false

// disable publishing of main docs
publishArtifact in (Compile, packageDoc) := false


// continuations
autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.11.2" % "1.0.2")

scalacOptions += "-P:continuations:enable"
