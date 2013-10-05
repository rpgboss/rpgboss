package rpgboss.player

import com.google.common.io.Files
import rpgboss._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import java.awt._
import java.awt.image._
import java.io.File
import javax.imageio.ImageIO

class MoveSpec extends UnitSpec {
  def fixture = {
    val tempDir = Files.createTempDir()
    val proj = Project.startingProject("TestProject", tempDir)
    
    val projStartup = proj.data.startup
    projStartup.soundCancel = None
    projStartup.soundCannot = None
    projStartup.soundCursor = None
    projStartup.soundSelect = None
    
    // Create a fake tileset with one passable white tile, one unpassable black.
    val image = new BufferedImage(64, 32, BufferedImage.TYPE_4BYTE_ABGR)
    val g = image.getGraphics()
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, 32, 32)
    g.setColor(Color.BLACK)
    g.fillRect(32, 0, 32, 32)

    val tilesetName = "testTileset.png"
    val tilesetImg = new File(Tileset.rcDir(proj), tilesetName)
    ImageIO.write(image, "png", tilesetImg) should equal(true)
    
    val tileset = Tileset.readFromDisk(proj, tilesetName)
    tileset.metadata.blockedDirsAry(0)(1) = DirectionMasks.ALLCARDINAL.toByte
    tileset.writeMetadata() should equal(true)
    
    // Create a fake map
    val mapName = RpgMap.generateName(1)
    val map = RpgMap.defaultInstance(proj, mapName)
    val mapData = RpgMap.emptyMapData(map.metadata.xSize, map.metadata.ySize)
    map.saveMapData(mapData) should equal(true)
    map.writeMetadata() should equal(true)
  }
}