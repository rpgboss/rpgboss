package rpgboss.model.resource

import rpgboss._
import rpgboss.model.resource._
import rpgboss.ProjectTest

class RpgMapSpec extends UnitSpec {
  "RpgMapData" should "be equal-comparable" in {
    val md1 = RpgMap.defaultMapData()
    val md2 = RpgMap.defaultMapData()
    md1 should equal (md2)
  }
  
  "RpgMapData" should "be persistable" in {
    val testMapName = "TestMap"
    val test = new ProjectTest
    val map1 = RpgMap.defaultInstance(test.project, testMapName)
    val md1 = RpgMap.defaultMapData()
    
    map1.writeMetadata() should equal (true)
    map1.saveMapData(md1) should equal (true)
    
    val map2 = RpgMap.readFromDisk(test.project, testMapName)
    map2.readMapData().isDefined should equal(true)
    val md2 = map2.readMapData().get
    
    md2 should equal (md1)
    map2 should equal (map1)
  }
}