package ch.epfl.scala.loadplugin

import java.io.File

import sbt.{Classpaths, Logger, State}
import sbt.internal.librarymanagement.mavenint.PomExtraDependencyAttributes
import sbt.librarymanagement._
import sbt.librarymanagement.ivy._
import sbt.librarymanagement.syntax._
import sbt.util.ShowLines._

object DependencyResolution {
  private final val DefaultResolvers =
    Vector(Resolver.defaultLocal,
           Resolver.mavenCentral,
           Resolver.sbtPluginRepo("releases"))

  private val scalaModuleInfo = Some(
    ScalaModuleInfo("2.12.5", "2.12", Vector.empty, false, false, false))

  /**
    * Resolve the specified module and get all the files. By default, the local ivy
    * repository and Maven Central are included in resolution.
    *
    * @param organization        The module's organization.
    * @param module              The module's name.
    * @param version             The module's version.
    * @param state               The current state of sbt
    * @param additionalResolvers Additional repositories to include in resolition.
    * @return All the files that compose the module and that could be found.
    */
  def resolve(organization: String,
              module: String,
              version: String,
              state: State,
              additionalResolvers: Seq[Resolver] = Seq.empty): Seq[File] = {
    val engine = getEngine(state, additionalResolvers)
    val moduleID = addExtraAttributes(organization % module % version)
    val moduleDescriptor =
      engine.wrapDependencyInModule(moduleID, scalaModuleInfo)
    engine.update(moduleDescriptor,
                  UpdateConfiguration(),
                  UnresolvedWarningConfiguration(),
                  state.log) match {
      case Left(unresolvedWarning) =>
        unresolvedWarning.lines.foreach(state.log.error(_))
        Seq.empty
      case Right(report) =>
        report.allFiles.filter(_.getName.endsWith(".jar"))
    }
  }

  private def addExtraAttributes(module: ModuleID): ModuleID =
    module
      .extra(PomExtraDependencyAttributes.SbtVersionKey -> "1.0",
             PomExtraDependencyAttributes.ScalaVersionKey -> "2.12")
      .withCrossVersion(Disabled())

  private def getEngine(state: State,
                        userResolvers: Seq[Resolver]): DependencyResolution = {
    val appConfiguration = state.configuration
    val ivyPaths = IvyPaths(appConfiguration.baseDirectory,
                            Classpaths.bootIvyHome(appConfiguration))
    val resolvers = DefaultResolvers ++ userResolvers.toVector
    val configuration = InlineIvyConfiguration()
      .withLock(appConfiguration.provider.scalaProvider.launcher.globalLock)
      .withPaths(ivyPaths)
      .withResolvers(resolvers)
      .withLog(state.log)
    IvyDependencyResolution(configuration)
  }
}
