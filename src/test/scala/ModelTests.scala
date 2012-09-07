import rpgboss.model._
import rpgboss.lib.FileHelper._

import org.scalatest._
import java.io._

class ModelSpec extends Spec with BeforeAndAfter {
  
  var tempdir: File = _
  var proj : Project = _
  def fakeByteAry() = { Array(Array[Byte](1,2,3), Array[Byte](1,4,5)) }
  
  before {
    tempdir = File.createTempFile("rpgtest", "tmp")
    tempdir.delete()
    tempdir.mkdir()
    proj = Project.startingProject("Test Project", tempdir)
  }
  
  after {
    tempdir.deleteAll()
  }
  
  describe("Project") {
    it("de/serialization") {
      val p0 = Project.startingProject("Test Project", tempdir)
      p0.writeMetadata()
      
      val p1 = Project.readFromDisk(tempdir).get
      
      assert(p0 === p1)
    }
  }
  
  describe("RpgMapData") {
    def makeMapData() =
      RpgMapData(fakeByteAry(), fakeByteAry(), fakeByteAry(), Array())
    
    def mapDataEquals(d0: RpgMapData, d1: RpgMapData) = {
      import java.util.Arrays
      val pairsToCompare = Array(
          d0.botLayer->d1.botLayer,
          d0.midLayer->d1.midLayer,
          d0.topLayer->d1.topLayer
      )
      
      // Assert all the maps are the same
      assert(pairsToCompare map {
        case (layer0, layer1) => 
          def sameLength = (layer0.length == layer1.length)
          def rowsSame = {
            val interleavedRows = layer0 zip layer1
            // Find if a row exists that doesn't match
            val mismatch = interleavedRows.exists {
              case (row0, row1) => !Array.equals(row0, row1)
            }
            // Return true if there's no mismatches,
            (!mismatch)
          }
          
          sameLength && rowsSame
      } reduceLeft (_ && _))
      
      // Assert rows are the same
      assert(d0.events.toList === d1.events.toList)
    }
    
    it("de/serialization") {
      val name = "RpgMapData-deserialization"
      val d0 = makeMapData()
      d0.writeToFile(proj, name)
      val d1 = RpgMapData.readFromDisk(proj, name).get
      
      mapDataEquals(d0, d1)
    }
    
    it("read/write by map") {
      val map = RpgMap.defaultInstance(proj, "RpgMapData-readwritefromMap")
      val d0 = makeMapData()
      map.saveMapData(d0)
      val d1 = map.readMapData().get
      mapDataEquals(d0, d1)
    }
  }
    
}

