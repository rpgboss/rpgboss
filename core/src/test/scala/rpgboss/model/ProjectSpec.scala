package rpgboss.model

import rpgboss._
import rpgboss.lib._
import com.google.common.io.Files
import rpgboss.model.event.RpgEventState
import rpgboss.model.event.AddRemoveItem
import rpgboss.model.event.EventClass
import rpgboss.model.event.IntParameter

class ProjectSpec extends UnitSpec {
  "ProjectDataStartup" should "be equal-comparable" in {
    val s1 = ProjectDataStartup()
    val s2 = ProjectDataStartup()
    s1 should deepEqual (s2)
  }

  "Project" should "be serializable" in {
    val fakeDirectory = Files.createTempDir()

    val p = Project.startingProject("fakeproject", fakeDirectory)

    // Mutate some values
    p.data.uuid = "fakeuid"
    p.data.startup.startingParty = Array(4)
    p.data.enums.characters.head.name = "New Test Name"
    p.data.enums.enemies.head.name = "New Enemy name"
    p.data.enums.eventClasses = Array(EventClass(
        name = "Test Event Class",
        states = Array(RpgEventState(
            cmds = Array(
                AddRemoveItem(
                    true,
                    IntParameter(constant = 1),
                    IntParameter(constant = 5)))))))

    p.writeMetadata() should equal (true)

    val pRead = Project.readFromDisk(fakeDirectory)
    pRead.isDefined should equal (true)
    pRead.get should deepEqual (p)
  }

}