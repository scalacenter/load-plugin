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
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    homepage := Some(url(s"https://github.com/scalacenter/load-plugin")),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("Duhemm",
                "Martin Duhem",
                "martin.duhem@gmail.com",
                url(s"https://github.com/Duhemm"))
    ),
    ghreleaseRepoOrg := "scalacenter",
    ghreleaseRepoName := "load-plugin",
    scriptedPackageTarget := target.value / s"scripted-${name.value}-${version.value}.jar",
    scriptedDependencies := {
      val bin = (packageBin in Compile).value
      IO.copyFile(bin, scriptedPackageTarget.value)
    },
    scriptedLaunchOpts := {
      Seq("-Xmx1024M", "-Dload.plugin.path=" + scriptedPackageTarget.value)
    }
  )
