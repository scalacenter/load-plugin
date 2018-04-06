package sbt

import sbt.Keys.baseDirectory
import sbt.internal.{Load, SessionSettings}

// Adapted from https://github.com/JetBrains/sbt-structure
// to support build settings
object ApplySettings {

  def applySettings(state: State,
                    globalSettings: Seq[Setting[_]],
                    buildSettings: Seq[Setting[_]],
                    projectSettings: Seq[Setting[_]]): State = {
    val extracted = Project.extract(state)
    import extracted.{structure => extractedStructure, _}
    val transformedBuildSettings = {
      val unit = extracted.currentUnit.unit
      val uri = unit.uri
      val buildScope = Scope(Select(BuildRef(uri)), Zero, Zero, Zero)
      val buildBase = Seq(baseDirectory :== unit.localBase)
      Load.transformSettings(buildScope,
                             uri,
                             extractedStructure.rootProject,
                             buildBase ++ buildSettings)
    }
    val transformedGlobalSettings =
      inScope(Scope(Zero, Zero, Zero, Zero))(globalSettings)
    val transformedProjectSettings = extractedStructure.allProjectRefs.flatMap {
      projectRef =>
        Load.transformSettings(Load.projectScope(projectRef),
                               projectRef.build,
                               rootProject,
                               projectSettings)
    }
    SessionSettings.reapply(
      extracted.session.appendRaw(
        transformedGlobalSettings ++ transformedBuildSettings ++ transformedProjectSettings),
      state)
  }

}
