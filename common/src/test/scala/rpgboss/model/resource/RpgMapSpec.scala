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
  
  "RpgMapData" should "have persistable events" in {
    val test = new ProjectTest
  }
}