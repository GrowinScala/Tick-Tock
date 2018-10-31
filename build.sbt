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
libraryDependencies += "org.quartz-scheduler" % "quartz" % "2.2.3"
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.3"
libraryDependencies += "org.yaml" % "snakeyaml" % "1.8"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.13"