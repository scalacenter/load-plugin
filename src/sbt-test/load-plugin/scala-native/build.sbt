commands += Command.command("do-apply") { state =>
  val cp = sys.props("load.plugin.path")
  s"apply -cp $cp ch.epfl.scala.loadplugin.LoadPlugin" :: state
}

scalaVersion := "2.11.11"
