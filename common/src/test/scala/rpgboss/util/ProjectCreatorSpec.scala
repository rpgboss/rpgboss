package rpgboss.util

import com.google.common.io.Files
import java.io.File
import rpgboss.UnitSpec

class ProjectCreatorSpec extends UnitSpec {
  "ProjectCreator" should "create all files" in {
    val tempDir = Files.createTempDir()
    ProjectCreator.create("test", tempDir)
    
    val cl = getClass.getClassLoader
    val rcStream = cl.getResourceAsStream("defaultrc/enumerated.txt")
    val resources = io.Source.fromInputStream(rcStream).getLines().toList

    for (resourceName <- resources) {
      val f = new File(tempDir, resourceName)
      f.exists() should equal (true)
      f.isFile() should equal (true)
      f.canRead() should equal (true)
    }

  }
}