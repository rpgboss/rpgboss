import rpgboss.model._
import rpgboss.lib.FileHelper._

import org.scalatest._
import java.io._

class ModelSpec extends Spec with BeforeAndAfter {
  
  var tempdir: File = _
  var proj : Project = _
  def fakeByteAry() = { Array[Byte](1,2,3,4,5) }
  
  before {
    tempdir = File.createTempFile("rpgtest", "tmp")
    tempdir.delete()
    tempdir.mkdir()
    proj = Project.startingProject("Test Project", tempdir)
  }
  
  after {
    tempdir.deleteAll()
  }

  describe("RpgModelData") {

    it("bytearray->str->bytearray") {
      val x0 = fakeByteAry()
      
      val strRep = RpgMapData.aryToStr(x0)
      val x1 = RpgMapData.strToAry(strRep)
      
      assert(x0 === x1)
    }
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
      assert(Arrays.equals(d0.botLayer, d1.botLayer))
      assert(Arrays.equals(d0.midLayer, d1.midLayer))
      assert(Arrays.equals(d0.topLayer, d1.topLayer))
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

