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
      RpgMapDataEventsIntermediate(events.values.toArray),
      RpgMapData.formats)

    mapFileWritten && layersWritten && eventsWritten
  }

  def resized(newXSize: Int, newYSize: Int) = {
    import RpgMap._

    val newLayers = List(botLayer, midLayer, topLayer) map { layerAry =>
      assert(!layerAry.isEmpty)
      assert(layerAry.head.length % RpgMap.bytesPerTile == 0)
      val oldXSize = layerAry.head.length / RpgMap.bytesPerTile
      assert(layerAry.forall(_.length == oldXSize * RpgMap.bytesPerTile))
      val oldYSize = layerAry.length

      // Expand or contract all the existing rows
      val newRowsSameYDim = layerAry.map { row =>
        val newRow =
          if (row.size > newXSize * bytesPerTile)
            row.take(newXSize * bytesPerTile)
          else
            row ++ makeRowArray(newXSize - oldXSize, RpgMap.emptyTileSeed)

        assert(newRow.length == newXSize * bytesPerTile)
        newRow
      }

      // Generate or destroy new rows
      if (newYSize < layerAry.size) {
        newRowsSameYDim.take(newYSize)
      } else {
        newRowsSameYDim ++ Array.fill(newYSize - oldYSize)({
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
    val eventsCopy = events.map {
      case (k, v) => k -> Utils.deepCopy(v)
    }

    copy(botLayer = botLayer.map(_.clone()),
         midLayer = midLayer.map(_.clone()),
         topLayer = topLayer.map(_.clone()),
         events = eventsCopy)
  }
}

case class RpgMapDataEventsIntermediate(events: Array[RpgEvent])

case object RpgMapData {
  val formats = Serialization.formats(EventCmd.hints + EventParameter.hints)

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

    for (botAry <- botAryOpt; midAry <- midAryOpt; topAry <- topAryOpt;
         eventsIntermediate <- eventsIntermediateOpt) yield {

      val events =
        eventsIntermediate.events.map(e => e.id->e).toMap

      RpgMapData(botAry, midAry, topAry, events)
    }
  }
}

