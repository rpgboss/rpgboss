package rpgboss.player

import rpgboss._

class VariablesSpec extends UnitSpec {
  "Integer variables" should "be set and get" in {
    val test = new MapScreenTest {
      def testScript() = {
        scriptInterface.setInt("testInt1", 42)
        scriptInterface.setInt("testInt2", -5)

        val retrieved1 = scriptInterface.getInt("testInt1")
        val retrieved2 = scriptInterface.getInt("testInt2")
        
        waiter {
          retrieved1 should equal (42)
          retrieved2 should equal (-5)
        }
      }
    }
    
    test.runTest()
  }
  
  "Arrays of integers" should "be set and get" in {
    val test = new MapScreenTest {
      def testScript() = {
        val testCase1 = Array[Int]()
        val testCase2 = Array(1, 2, 5)
        
        scriptInterface.setIntArray("testCase1", testCase1)
        scriptInterface.setIntArray("testCase2", testCase2)
        
        val retrieved1 = scriptInterface.getIntArray("testCase1")
        val retrieved2 = scriptInterface.getIntArray("testCase2")
        
        waiter {
          retrieved1 should equal (testCase1)
          retrieved2 should equal (testCase2)
        }
      }
    }
    
    test.runTest()
  }
  
  "Arrays of strings" should "be set and get" in {
    val test = new MapScreenTest {
      def testScript() = {
        val testCase1 = Array[String]()
        val testCase2 = Array("one", "two", "five")
        
        scriptInterface.setStringArray("testCase1", testCase1)
        scriptInterface.setStringArray("testCase2", testCase2)
        
        val retrieved1 = scriptInterface.getStringArray("testCase1")
        val retrieved2 = scriptInterface.getStringArray("testCase2")
        
        waiter {
          retrieved1 should equal (testCase1)
          retrieved2 should equal (testCase2)
        }
      }
    }
    
    test.runTest()
  }
}