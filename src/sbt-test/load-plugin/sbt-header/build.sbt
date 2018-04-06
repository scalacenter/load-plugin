commands += Command.command("do-apply") { state =>
  val cp = sys.props("load.plugin.path")
  s"apply -cp $cp ch.epfl.scala.loadplugin.LoadPlugin" :: state
}

organizationName := "Scala Center"
startYear := Some(2018)
licenses += ("Apache-2.0", new URL(
  "https://www.apache.org/licenses/LICENSE-2.0.txt"))
