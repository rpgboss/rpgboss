package rpgboss.lib

import rpgboss._

class UtilsSpec extends UnitSpec {
  "ArrayUtils.resized" should "fill with new objects for each element" in {
    case class TestClass()
    
    val orig = Seq(TestClass())
    val resized = ArrayUtils.resized(orig, 3, TestClass.apply _)
    
    resized(1) should not be theSameInstanceAs (resized(2))
  }

}