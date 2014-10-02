package rpgboss.save

import rpgboss.UnitSpec
import rpgboss.ProjectTest
import rpgboss.model.MapLoc

class SaveFileSpec extends UnitSpec {
  "SaveFile" should "read an empty one for no file existing" in {
    val test = new ProjectTest
    SaveFile.read(test.project) should deepEqual(SaveFile())
  }

  "SaveFile" should "serialize and deserialize correctly" in {
    val test = new ProjectTest
    val fakeSaveFile = SaveFile(
      Map("five" -> 5, "six" -> 6),
      Map("foo" -> Array(5, 6, 8)),
      Map("five" -> Array("five")),
      Map("player" -> MapLoc("town", 5.5f, 2.5f)),
      Array(SavedEventState("map1", 1, 5), SavedEventState("map2", 4, 6)))

    SaveFile.write(fakeSaveFile, test.project) should equal(true)
    SaveFile.read(test.project) should deepEqual(fakeSaveFile)
  }
}