package rpgboss.util

import com.google.common.io.Files
import java.io.File
import rpgboss.UnitSpec
import rpgboss.model.resource.Resource

class ProjectCreatorSpec extends UnitSpec {
  "ProjectCreator" should "create all json files" in {
    val tempDir = Files.createTempDir()
    ProjectCreator.create("test", tempDir)

    val cl = getClass.getClassLoader
    val rcStream = cl.getResourceAsStream("defaultrc/enumerated.txt")
    val resources = io.Source.fromInputStream(rcStream).getLines().toList

    for (fileName <- resources;
         if fileName.endsWith(Resource.jsonSuffix) && !fileName.contains("/")) {
      val f = new File(tempDir, fileName)
      f.exists() should equal (true)
      f.isFile() should equal (true)
      f.canRead() should equal (true)
    }

  }
}