package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import net.iharder.Base64

import java.io._
import java.util.zip._

case class Event()

// this class has mutable members
case class RpgMapData(botLayer: Array[Byte],
                      midLayer: Array[Byte],
                      topLayer: Array[Byte],
                      events: Array[Event])
{
  import RpgMapData._
  def drawOrder = List(botLayer, midLayer, topLayer)
  
  def toIntermediate = 
    RpgMapDataIntermediate(aryToStr(botLayer),
                           aryToStr(midLayer),
                           aryToStr(topLayer),
                           events)
  
  def writeToFile(p: Project, name: String) =
    RpgMapData.dataFile(p, name).getWriter().map { writer =>
      implicit val formats = net.liftweb.json.DefaultFormats
      Serialization.write(this.toIntermediate, writer) != null
    } getOrElse false
}

// Actually jsonable case class
case class RpgMapDataIntermediate(botLayerStr: String,
                                  midLayerStr: String,
                                  topLayerStr: String,
                                  events: Array[Event])

case object RpgMapData {
  def dataFile(p: Project, name: String) = 
    new File(p.mapsDir, "%s.mapdata.json".format(name))
  
  def readFromDisk(proj: Project, name: String) : Option[RpgMapData] = {
    implicit val formats = net.liftweb.json.DefaultFormats
    dataFile(proj, name).getReader().map { reader => 
      val intermediate = Serialization.read[RpgMapDataIntermediate](reader)
      RpgMapData(strToAry(intermediate.botLayerStr),
                 strToAry(intermediate.midLayerStr),
                 strToAry(intermediate.topLayerStr),
                 intermediate.events)
    }
  }
  
  def aryToStr(ary: Array[Byte]) = {
    val byteOutput = new ByteArrayOutputStream()
    val gzipOutput = new GZIPOutputStream(new Base64.OutputStream(byteOutput))
    
    gzipOutput.write(ary, 0, ary.length)
    byteOutput.toString("ASCII")
  }
  
  def strToAry(str: String) = {
    val compressedAry = Base64.decode(str)
    val gzipInputStream = new GZIPInputStream(
      new ByteArrayInputStream(compressedAry))
    
    val buffer = new Array[Byte](1024*32)
    val outputBytes = new ByteArrayOutputStream()
    
    Iterator.continually(gzipInputStream.read(buffer))
      .takeWhile(_ != -1).foreach(outputBytes.write(buffer, 0, _))
    
    outputBytes.toByteArray()
  }
}
