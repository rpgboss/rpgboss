package rpgboss.util

import com.google.common.io.Files
import com.google.common.io.Resources
import java.io._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.lib._
import java.util.Arrays

object ProjectCreator {
  def copyResources(resourceDirectoryName: String,
                    resourceList: Seq[String], targetRcDirectory: File,
                    copyAllFiles: Boolean = false) = {
    for (resourceName <- resourceList) {
      val target = new File(targetRcDirectory, resourceName)
      target.getParentFile.mkdirs()

      if (copyAllFiles ||
          (!resourceName.contains("/") &&
              resourceName.endsWith(Resource.jsonSuffix))) {
        Utils.copyResource(
          "%s/%s".format(resourceDirectoryName, resourceName), target)
      }
    }
  }

  // Returns true if project has been created and all default resources copied
  def create(shortname: String, projectDirectory: File): Option[Project] = {
    val projectStub = Project.startingProject(shortname, projectDirectory)
    val mapName = RpgMap.generateName(projectStub.data.lastCreatedMapId)

    projectDirectory.mkdir()
    Resource.resourceTypes.foreach {
      _.rcDir(projectStub).mkdir()
    }

    val allSavedOkay =
      projectStub.data.writeRootWithoutEnums(projectStub.dir) &&
        RpgMap.defaultMapData.writeToFile(projectStub, mapName) &&
        RpgMap.defaultInstance(projectStub, mapName).writeMetadata()

    val cl = getClass.getClassLoader
    val projRcDir = projectStub.rcDir

    copyResources(
      ResourceConstants.defaultRcDir,
      ResourceConstants.defaultRcList,
      projRcDir)

    if (allSavedOkay) {
      Project.readFromDisk(projectStub.dir)
    } else {
      None
    }
  }
}