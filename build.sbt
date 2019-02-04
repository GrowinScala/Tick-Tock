name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.7"

crossScalaVersions := Seq("2.11.12", "2.12.7")

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.197"

libraryDependencies += "org.flywaydb" % "flyway-core" % "5.2.1"
libraryDependencies += "com.enragedginger" %% "akka-quartz-scheduler" % "1.7.0-akka-2.5.x"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.13"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % Test

//mainClass in (Compile, run) := Some("api.services.TickTock")
//mainClass in run := Some("api.services.TickTock.main")