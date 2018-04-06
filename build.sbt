val scriptedPackageTarget = settingKey[File](
  "Target for the package artifact when running scripted tests")

val loadPlugin = project
  .in(file("."))
  .settings(
    name := "load-plugin",
    organization := "ch.epfl.scala",
    description := "Utility to programatically load sbt plugins",
    scalaVersion := "2.12.5",
    libraryDependencies += "org.scala-sbt" % "sbt" % "1.1.2" % Provided,
    scriptedPackageTarget := target.value / s"scripted-${name.value}-${version.value}.jar",
    scriptedDependencies := {
      val bin = (packageBin in Compile).value
      IO.copyFile(bin, scriptedPackageTarget.value)
    },
    scriptedLaunchOpts := {
      Seq("-Xmx1024M", "-Dload.plugin.path=" + scriptedPackageTarget.value)
    }
  )
