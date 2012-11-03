package rpgboss.model

import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.lib.FileHelper._
import net.liftweb.json.Serialization
import java.io._
import rpgboss.model.resource.RpgMap
import net.liftweb.json.ShortTypeHints

/*
 * This class has mutable members.
 * 
 * See RpgMap object for an explanation of the data format.
 * 
 * botLayer, midLayer, and topLayer must always be of size at least 1 x 1
 */
case class RpgMapData(botLayer: Array[Array[Byte]],
                      midLayer: Array[Array[Byte]],
                      topLayer: Array[Array[Byte]],
                      events: Array[RpgEvent],
                      lastGeneratedEventId: Int = 0)
{
  import RpgMapData._
  def drawOrder = List(botLayer, midLayer, topLayer)
  
  def toIntermediate = 
    RpgMapDataIntermediate(botLayer.map(_.map(_.toInt)),
                           midLayer.map(_.map(_.toInt)),
                           topLayer.map(_.map(_.toInt)),
                           events)
  
  def writeToFile(p: Project, name: String) =
    RpgMapData.dataFile(p, name).useWriter { writer =>
      implicit val formats = RpgMapData.formats
      Serialization.writePretty(this.toIntermediate, writer) != null
    } getOrElse false
  
  def resized(newXSize: Int, newYSize: Int) = {
    import RpgMap._
    
    val newLayers = List(botLayer, midLayer, topLayer) map { layerAry =>
      // Expand or contract all the existing rows
      val newRowsSameYDim = layerAry.map { row =>
        if(row.size > newXSize)
          row.take(newXSize*bytesPerTile)
        else
          row ++ makeRowArray(newXSize-row.size, RpgMap.emptyTileSeed)
      }
      
      // Generate or destroy new rows
      if(newYSize < layerAry.size) {
        newRowsSameYDim.take(newYSize)
      } else {
        newRowsSameYDim ++ Array.fill(newYSize-layerAry.size)({
          makeRowArray(newXSize, RpgMap.emptyTileSeed)
        })
      }
    }
    
    copy(
        botLayer = newLayers(0), 
        midLayer = newLayers(1), 
        topLayer = newLayers(2))
  }
}

// Actually jsonable case class
case class RpgMapDataIntermediate(botLayer: Array[Array[Int]],
                                  midLayer: Array[Array[Int]],
                                  topLayer: Array[Array[Int]],
                                  events: Array[RpgEvent])

case object RpgMapData {
  def dataFile(p: Project, name: String) = 
    new File(RpgMap.rcDir(p), name)
  
  val formats = Serialization.formats(ShortTypeHints(
      EventCmd.types))
  
  def readFromDisk(proj: Project, name: String) : Option[RpgMapData] = {
    implicit val formats = RpgMapData.formats
    dataFile(proj, name).getReader().map { reader => 
      val intermediate = Serialization.read[RpgMapDataIntermediate](reader)
      RpgMapData(intermediate.botLayer.map(_.map(_.toByte)),
                 intermediate.midLayer.map(_.map(_.toByte)),
                 intermediate.topLayer.map(_.map(_.toByte)),
                 intermediate.events)
    }
  }
}
