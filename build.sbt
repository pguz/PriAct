lazy val commonSettings = Seq(
  organization := "com.wedt.priact",
  version := "0.1",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "PriAct",

    libraryDependencies ++= {
      val akkaVersion = "2.3.9"
      Seq(
        ("com.typesafe.akka" %% "akka-actor" % akkaVersion),
        ("org.jsoup" % "jsoup" % "1.8.1"),
        ("org.scala-lang.modules" %% "scala-swing" % "1.0.1"),
        ("io.reactivex" %% "rxscala" % "0.23.0"),
        ("io.reactivex" % "rxswing" % "0.21.0"), // for Swing Scheduler in suggestions
        ("com.typesafe.slick" %% "slick" % "2.1.0"),
        ("com.h2database" % "h2" % "1.3.175")
      )

    }
  )
