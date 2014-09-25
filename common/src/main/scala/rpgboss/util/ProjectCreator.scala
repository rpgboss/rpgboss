package rpgboss.util

import com.google.common.io.Files
import com.google.common.io.Resources
import java.io._
import rpgboss.model._
import rpgboss.model.resource._

object ProjectCreator {
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

    val copiedAllResources = {
      val projRcDir = projectStub.rcDir

      for (resourceName <- ResourceConstants.defaultRcList;
           if resourceName.endsWith(Resource.jsonSuffix)) {

        val source = Resources.asByteSource(
            Resources.getResource("%s/%s".format(
                ResourceConstants.defaultRcDir, resourceName)))

        val target = new File(projRcDir, resourceName)
        target.getParentFile.mkdirs()

        source.copyTo(Files.asByteSink(target))
      }

      true
    }

    if (allSavedOkay && copiedAllResources) {
      Project.readFromDisk(projectStub.dir)
    } else {
      None
    }
  }
}