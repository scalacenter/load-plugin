commands += Command.command("do-apply") { state =>
  val cp = sys.props("load.plugin.path")
  s"apply -cp $cp ch.epfl.scala.loadplugin.LoadPlugin" :: state
}

name := "foobar"
libraryDependencies += "org.typelevel" %% "cats-core" % "1.1.0"
