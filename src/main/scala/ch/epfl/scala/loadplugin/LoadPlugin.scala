package ch.epfl.scala.loadplugin

import sbt._
import sbt.complete.DefaultParsers._
import sbt.internal.inc.ModuleUtilities
import sbt.internal.inc.classpath.ClasspathUtilities.toLoader

object LoadPlugin extends (State => State) {

  private val LoadPlugin = "load-plugin"
  private val IfAbsent = "if-absent"

  // Parse two strings separated by a space.
  private def moduleParser =
    token(Space) ~> NotSpace ~ (token(Space) ~> NotSpace)

  private def ifAbsentParser =
    token(Space) ~> NotSpace ~ (token(Space) ~> repsep(StringBasic,
                                                       token(Space)))

  /**
    * An sbt command that resolves a plugin and loads it inside the build.
    *
    * This commands takes two strings as parameters:
    *  - The moduleID of the plugin to resolve, written as `org:name:version`.
    *  - The fully qualified class name of the object that extends `AutoPlugin`.
    *
    * This command returns a new state where the settings injected by the loaded
    * plugins are applied.
    */
  private def loadPluginCommand: Command = {
    Command(LoadPlugin, Help.empty)(_ => moduleParser) {

      case (state, (module, fqcn)) =>
        state.log.info(s"Resolving and loading $module")
        val Array(org, name, version) = module.split(":")
        val files =
          DependencyResolution.resolve(org, name, version, state)

        if (files.nonEmpty) {
          state.log.debug("Downloaded:")
          files.foreach(f => state.log.debug(s" * ${f.getAbsolutePath}"))
          val loader = toLoader(files, getClass.getClassLoader)

          // TODO: Find out plugin name by myself
          val loaded =
            ModuleUtilities.getObject(fqcn, loader).asInstanceOf[AutoPlugin]

          val newState = ApplySettings.applySettings(state,
                                                     loaded.globalSettings,
                                                     loaded.buildSettings,
                                                     loaded.projectSettings)
          newState.log.success(s"Loaded plugin `${loaded.label}`.")
          newState
        } else {
          state
        }
    }
  }

  /**
    * An sbt command that will run one or more commands if a plugin is not loaded in the build. This
    * command cannot be used to check whether plugins have been loaded using the `load-plugin`
    * command.
    *
    * This commands takes at least 2 arguments: a fully qualified class name and one or more commands
    * to execute if the plugin specified is not loaded.
    */
  private def ifAbsentCommand: Command = {
    Command(IfAbsent, Help.empty)(_ => ifAbsentParser) {

      case (state, (fqcn, commands)) =>
        val extracted = Project.extract(state)
        val structure = extracted.structure
        val pluginsClasses = for {
          (_, unit) <- structure.units
          plugins = unit.unit.plugins.detected.autoPlugins
          plugin <- plugins
        } yield plugin.value.getClass.getName

        state.log.debug("Plugins loaded in the build:")
        pluginsClasses.foreach(p => state.log.debug(s" - $p"))

        if (pluginsClasses.toList.contains(fqcn + "$")) {
          state.log.success(s"$fqcn is loaded; skipping.")
          state
        } else {
          commands.toList ::: state
        }
    }
  }

  /**
    * Inject the `load-plugin` command inside the input `State`.
    *
    * This function is meant to be called by sbt when invoking the `apply` command.
    *
    * @param state The current state
    * @return The same state, with the `load-plugin` command injected.
    */
  override def apply(state: State): State = {
    val newState =
      state.copy(
        definedCommands = state.definedCommands ++ Seq(loadPluginCommand,
                                                       ifAbsentCommand))
    newState.log.success(s"Injected `$LoadPlugin`")
    newState
  }

}
