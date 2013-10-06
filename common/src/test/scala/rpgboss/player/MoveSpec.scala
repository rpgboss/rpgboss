package rpgboss.player

import com.google.common.io.Files
import java.awt._
import java.awt.image._
import java.io.File
import javax.imageio.ImageIO
import rpgboss._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player._
import rpgboss.player.entity.EntityMove
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.PatienceConfiguration._
import org.scalatest.time._

class MoveSpec extends UnitSpec {
  def paint(array: Array[Array[Byte]], x: Int, y: Int, bytes: Array[Byte]) = {
    array.length should be > (0)
    array.head.length should be > (0)
    x should be >= (0)
    x should be < array.head.length
    y should be >= (0)
    y should be < array.length
    bytes.length should equal(RpgMap.bytesPerTile)
    
    for (i <- 0 until RpgMap.bytesPerTile) {
      array(y)(x * RpgMap.bytesPerTile + i) = bytes(i)
    }
  }
  
  def paintPassable(array: Array[Array[Byte]], x: Int, y: Int) = {
    paint(array, x, y, Array(RpgMap.autotileByte, 16, 0))
  }
  
  def fixture() = {
    val tempDir = Files.createTempDir()
    val projectOption = rpgboss.util.ProjectCreator.create("test", tempDir)
    projectOption should be ('isDefined)
    
    val proj = projectOption.get
    
    new {
      val projectDirectory = tempDir
      val project = proj
      val mapName = RpgMap.generateName(proj.data.lastCreatedMapId)
    }
  }
  
  "Move" should "move right simple" in {
    val f = fixture()
    val w = new Waiter
    
    val game = new TestGame(f.projectDirectory, w) {
      def setup() = {
        val map = RpgMap.readFromDisk(project, f.mapName)
        val mapData = map.readMapData().get
        for (x <- 0 until 20; y <- 0 until 20)
          paintPassable(mapData.botLayer, x, y)
        map.saveMapData(mapData) should be (true)
      }
      
      def runTest() = {
        scriptInterface.setNewGameVars()
        scriptInterface.setPlayerLoc(MapLoc(f.mapName, 5.5f, 5.5f));
        val player = scriptInterface.getPlayerEntity()
        scriptInterface.moveEntity(player, 4f, 0)
        
        w {
          val epsilon = 0.05f
          player.x should be (9.5f +- epsilon)
          player.y should be (5.5f +- epsilon)
        }
        
        w.dismiss()
      }
    }
    
    val app = TestPlayer.launch(game)
    w.await(Timeout(Span(10, Seconds)))
    app.exit()
    game.awaitExit()
  }
}