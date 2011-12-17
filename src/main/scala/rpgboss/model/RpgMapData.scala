package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import net.iharder.Base64

import java.io._

case class RpgEvent()

// this class has mutable members
case class RpgMapData(botLayer: Array[Byte],
                      midLayer: Array[Byte],
                      topLayer: Array[Byte],
                      events: Array[RpgEvent])
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
                                  events: Array[RpgEvent])

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
  
  def aryToStr(ary: Array[Byte]) = Base64.encodeBytes(ary, Base64.GZIP)
  def strToAry(str: String) = Base64.decode(str)
}
