package rpgboss.util

import com.google.common.io.Files
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

      val rcStream = cl.getResourceAsStream("defaultrc/enumerated.txt")
      val resources = io.Source.fromInputStream(rcStream).getLines().toList

      for (resourceName <- resources) {

        val target = new File(projRcDir, resourceName)

        target.getParentFile.mkdirs()

        val fos = new FileOutputStream(target)

        val buffer = new Array[Byte](1024 * 32)

        val sourceStream =
          cl.getResourceAsStream("defaultrc/%s".format(resourceName))

        Iterator.continually(sourceStream.read(buffer))
          .takeWhile(_ != -1).foreach(fos.write(buffer, 0, _))
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