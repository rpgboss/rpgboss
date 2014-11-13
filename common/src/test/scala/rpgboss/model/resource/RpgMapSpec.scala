package rpgboss.model.resource

import rpgboss._
import rpgboss.model.resource._
import rpgboss.model.event._
import rpgboss.model._

class RpgMapSpec extends UnitSpec {
  "RpgMapData" should "be equal-comparable" in {
    val md1 = RpgMap.defaultMapData()
    val md2 = RpgMap.defaultMapData()
    md1 should deepEqual (md2)
  }

  "RpgMapData" should "default instance should have distinct arrays" in {
    val md = RpgMap.defaultMapData()
    md.botLayer should not be theSameInstanceAs(md.midLayer)
    md.botLayer should not be theSameInstanceAs(md.topLayer)
    md.midLayer should not be theSameInstanceAs(md.topLayer)
  }

  "RpgMapData" should "resize correctly" in {
    val md = RpgMap.defaultMapData()

    def testDimension(data: RpgMapData, expectedW: Int, expectedH: Int) = {
      for (layer <- data.drawOrder) {
        layer.length should equal(expectedH)
        for (row <- layer) {
          row.length should equal(expectedW * RpgMap.bytesPerTile)
        }
      }
    }

    testDimension(md, RpgMap.initXSize, RpgMap.initYSize)
    testDimension(md.resized(10, 20), 10, 20)
    testDimension(md.resized(50, RpgMap.initYSize), 50, RpgMap.initYSize)
    testDimension(md.resized(RpgMap.initXSize, 50), RpgMap.initXSize, 50)
    testDimension(md.resized(120, 160), 120, 160)
  }

  "RpgMapData" should "be persistable" in {
    val testMapName = "TestMap.rpgmap"
    val test = new ProjectTest
    val map1 = RpgMap.defaultInstance(test.project, testMapName)
    val md1 = RpgMap.defaultMapData()

    // mutate some stuff
    map1.metadata.editorCenterX = 42

    val state = RpgEventState(sprite = Some(SpriteSpec("testSpriteName", 0)))
    md1.events = Map(1 -> RpgEvent(
        1, "TestEvent", 5f, 5f, Array(state),
        Map()))
    md1.eventInstances = Map(2-> EventInstance(
        5, 2, "InstanceName", 4f, 4f,
        Map()))

    // Add some differing data to each layer.
    md1.botLayer(1)(1) = 1
    md1.midLayer(1)(1) = 2
    md1.topLayer(1)(1) = 3

    map1.writeMetadata() should equal (true)
    map1.saveMapData(md1) should equal (true)

    val map2 = RpgMap.readFromDisk(test.project, testMapName)
    map2.readMapData().isDefined should equal(true)
    val md2 = map2.readMapData().get

    md2.events should deepEqual (md1.events)
    md2 should deepEqual (md1)
    map2 should deepEqual (map1)
  }
}