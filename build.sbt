name := """Tick-Tock"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(

   "commons-io" % "commons-io" % "2.6",
   "com.enragedginger" %% "akka-quartz-scheduler" % "1.7.0-akka-2.5.x",
   "com.h2database" % "h2" % "1.4.197" % Test,
   "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % Test,
   "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
   guice,
   "mysql" % "mysql-connector-java" % "8.0.13",
   "org.flywaydb" % "flyway-core" % "5.2.1",
   "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
  
)