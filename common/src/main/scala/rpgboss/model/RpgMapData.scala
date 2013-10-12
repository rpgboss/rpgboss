package rpgboss.model

import java.io._
import au.com.bytecode.opencsv.{ CSVReader, CSVWriter }
import org.json4s.native.Serialization
import org.json4s.ShortTypeHints
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.lib.FileHelper._
import rpgboss.model.resource.RpgMap
import scala.collection.mutable.ArrayBuffer

/*
 * This class has mutable members.
 * 
 * See RpgMap object for an explanation of the data format.
 * 
 * botLayer, midLayer, and topLayer must always be of size at least 1 x 1
 */
case class RpgMapData(botLayer: ArrayBuffer[ArrayBuffer[Byte]],
                      midLayer: ArrayBuffer[ArrayBuffer[Byte]],
                      topLayer: ArrayBuffer[ArrayBuffer[Byte]],
                      var events: Map[Int, RpgEvent],
                      var lastGeneratedEventId: Int = 0) {
  import RpgMapData._
  def drawOrder = List(botLayer, midLayer, topLayer)
  
  def writeCsv(file: File, data: Seq[Seq[Byte]]) = {
    val writer =
      new CSVWriter(new FileWriter(file), '\t', CSVWriter.NO_QUOTE_CHARACTER)

    data.foreach(row =>
      writer.writeNext(row.map(b => (b & 0xff).toString).toArray))

    writer.close()
    true
  }

  def writeToFile(p: Project, name: String) = {
    val (mapFile, botFile, midFile, topFile, evtFile) = datafiles(p, name)

    val mapFileWritten = mapFile.isFile() || mapFile.createNewFile()

    val layersWritten = writeCsv(botFile, botLayer) &&
      writeCsv(midFile, midLayer) &&
      writeCsv(topFile, topLayer)

    val eventsWritten = evtFile.useWriter { writer =>
      implicit val formats = RpgMapData.formats
      Serialization.writePretty(
          RpgMapDataEventsIntermediate(events), writer) != null
    } getOrElse false

    layersWritten && eventsWritten
  }

  def resized(newXSize: Int, newYSize: Int) = {
    import RpgMap._

    val newLayers = List(botLayer, midLayer, topLayer) map { layerAry =>
      // Expand or contract all the existing rows
      val newRowsSameYDim = layerAry.map { row =>
        if (row.size > newXSize)
          row.take(newXSize * bytesPerTile)
        else
          row ++ makeRowArray(newXSize - row.size, RpgMap.emptyTileSeed)
      }

      // Generate or destroy new rows
      if (newYSize < layerAry.size) {
        newRowsSameYDim.take(newYSize)
      } else {
        newRowsSameYDim ++ Array.fill(newYSize - layerAry.size)({
          makeRowArray(newXSize, RpgMap.emptyTileSeed)
        })
      }
    }

    copy(
      botLayer = newLayers(0),
      midLayer = newLayers(1),
      topLayer = newLayers(2))
  }

  def deepcopy() = {
    copy(events = events.mapValues(_.copy()))
  }
}

case class RpgMapDataEventsIntermediate(events: Seq[RpgEvent])
object RpgMapDataEventsIntermediate {
  def apply(events: Map[Int, RpgEvent]): RpgMapDataEventsIntermediate = 
    apply(events.values.toArray)
}

case object RpgMapData {
  val formats = Serialization.formats(ShortTypeHints(
    EventCmd.types))

  def datafiles(p: Project, name: String) = {
    val mapFile = new File(RpgMap.rcDir(p), name)
    val botFile = new File(RpgMap.rcDir(p), name + ".bot.csv")
    val midFile = new File(RpgMap.rcDir(p), name + ".mid.csv")
    val topFile = new File(RpgMap.rcDir(p), name + ".top.csv")
    val evtFile = new File(RpgMap.rcDir(p), name + ".evt.json")

    (mapFile, botFile, midFile, topFile, evtFile)
  }

  def readCsvArray(file: File) = {
    val reader = new CSVReader(new FileReader(file), '\t')

    val buffer = new ArrayBuffer[ArrayBuffer[Byte]]()

    Iterator.continually(reader.readNext()).takeWhile(_ != null).map { row =>
      buffer.append(ArrayBuffer(row.map(_.toInt.toByte) : _*))
    }

    reader.close()

    buffer
  }

  def readFromDisk(p: Project, name: String): Option[RpgMapData] = {
    val (_, botFile, midFile, topFile, evtFile) = datafiles(p, name)

    val botAry = readCsvArray(botFile)
    val midAry = readCsvArray(midFile)
    val topAry = readCsvArray(topFile)

    implicit val formats = RpgMapData.formats

    evtFile.getReader().map { reader =>    
      val intermediate = 
        Serialization.read[RpgMapDataEventsIntermediate](reader)
      val events = intermediate.events.map(event => event.id->event).toMap
      RpgMapData(botAry, topAry, midAry, events)
    }
  }
}

