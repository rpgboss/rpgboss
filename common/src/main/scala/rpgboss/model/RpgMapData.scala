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
import rpgboss.model.resource.RpgMapMetadata

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
                      var events: Map[Int, RpgEvent]) {
  import RpgMapData._
  def drawOrder = List(botLayer, midLayer, topLayer)

  /**
   * Removes all the invalid tiles. Allows for an optimized player engine.
   */
  def sanitizeForMetadata(metadata: RpgMapMetadata) = {
    for (layerAry <-
         List(botLayer, midLayer, topLayer)) {
      for (tileY <- 0 until metadata.ySize) {
        val row = layerAry(tileY)
        import RpgMap.bytesPerTile
        for (tileX <- 0 until metadata.xSize) {
          val idx = tileX * bytesPerTile
          val byte1 = row(idx)
          val byte2 = row(idx + 1)
          val byte3 = row(idx + 2)

          if (byte1 < 0) {

          } else { // Regular tile
            if (byte1 >= metadata.tilesets.length) {
              row(idx) = RpgMap.emptyTileByte
            }
          }
        }
      }
    }
  }

  def writeCsv(file: File, data: Array[Array[Byte]]) = {
    val writer =
      new CSVWriter(new FileWriter(file), '\t', CSVWriter.NO_QUOTE_CHARACTER)

    data.foreach(row =>
      writer.writeNext(row.map(b => b.toString).toArray))

    writer.close()
    true
  }

  def writeToFile(p: Project, name: String) = {
    val (mapFile, botFile, midFile, topFile, evtFile) = datafiles(p, name)

    val mapFileWritten = mapFile.isFile() || mapFile.createNewFile()

    val layersWritten = writeCsv(botFile, botLayer) &&
      writeCsv(midFile, midLayer) &&
      writeCsv(topFile, topLayer)

    val eventsWritten = JsonUtils.writeModelToJsonWithFormats(
      evtFile,
      RpgMapDataEventsIntermediate(events),
      RpgMapData.formats)

    mapFileWritten && layersWritten && eventsWritten
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
    copy(botLayer = botLayer.map(_.clone()),
         midLayer = midLayer.map(_.clone()),
         topLayer = topLayer.map(_.clone()),
         events = events.mapValues(_.copy()))
  }
}

case class RpgMapDataEventsIntermediate(events: Array[RpgEvent])
object RpgMapDataEventsIntermediate {
  def apply(events: Map[Int, RpgEvent]): RpgMapDataEventsIntermediate =
    apply(events.values.toArray)
}

case object RpgMapData {
  val formats = Serialization.formats(ShortTypeHints(EventCmd.types))

  def datafiles(p: Project, name: String) = {
    val mapFile = new File(RpgMap.rcDir(p), name)
    val botFile = new File(RpgMap.rcDir(p), name + ".bot.csv")
    val midFile = new File(RpgMap.rcDir(p), name + ".mid.csv")
    val topFile = new File(RpgMap.rcDir(p), name + ".top.csv")
    val evtFile = new File(RpgMap.rcDir(p), name + ".evt.json")

    (mapFile, botFile, midFile, topFile, evtFile)
  }

  def readCsvArray(file: File): Option[Array[Array[Byte]]]= {
    val reader = new CSVReader(new FileReader(file), '\t')
    val buffer = new ArrayBuffer[ArrayBuffer[Byte]]()

    val csvIt = Iterator.continually(reader.readNext()).takeWhile(_ != null)

    for (row <- csvIt) {
      buffer.append(ArrayBuffer(row.map(_.toInt.toByte): _*))
    }

    reader.close()

    Some(buffer.map(_.toArray).toArray)
  }

  def readFromDisk(p: Project, name: String): Option[RpgMapData] = {
    val (_, botFile, midFile, topFile, evtFile) = datafiles(p, name)

    val botAryOpt = readCsvArray(botFile)
    val midAryOpt = readCsvArray(midFile)
    val topAryOpt = readCsvArray(topFile)

    val eventsIntermediateOpt =
      JsonUtils.readModelFromJsonWithFormats[RpgMapDataEventsIntermediate](
        evtFile, RpgMapData.formats)
    val eventsOpt = eventsIntermediateOpt.map { intermediate =>
      intermediate.events.map(event => event.id->event).toMap
    }

    for (botAry <- botAryOpt; midAry <- midAryOpt; topAry <- topAryOpt;
         events <- eventsOpt) yield {

      // TODO: Move to a more mature system for schema migration.
      val fixedEvents = events.map {
        case (k, event) => {
          val newStates = event.states.map { state =>
            val newCmds = state.cmds.filter(!_.isInstanceOf[EndOfScript])
            state.copy(cmds = newCmds)
          }
          (k, event.copy(states = newStates))
        }
      }

      RpgMapData(botAry, midAry, topAry, fixedEvents)
    }
  }
}

