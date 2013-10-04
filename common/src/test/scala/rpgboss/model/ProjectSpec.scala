package rpgboss.model

import rpgboss._

class ProjectSpec extends UnitSpec {
  "ProjectDataStartup" should "be equal-comparable" in {
    val s1 = ProjectDataStartup()
    val s2 = ProjectDataStartup()
    s1 should equal (s2)
  }
  
  
  
  "Project" should "be serializable" in {
    val fakeDirectory = java.io.File.createTempFile("rpgboss", "fakeproject")
    
    val p = Project.startingProject("fakeproject", fakeDirectory)
    p.writeMetadata() should equal (true)
    
    val pRead = Project.readFromDisk(fakeDirectory)
    pRead.isDefined should equal (true)
    pRead.get should equal (p)
  }

}