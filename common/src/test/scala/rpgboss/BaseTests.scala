package rpgboss

import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import com.google.common.io.Files
import org.lwjgl.opengl.Display
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.PatienceConfiguration._
import org.scalatest.time._
import org.scalatest._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.model.resource._
import rpgboss.player._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class ProjectTest extends ShouldMatchers {
  def paint(array: Array[Array[Byte]], x: Int, y: Int,
            bytes: Array[Byte]) = {
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

  def singleTestEvent(cmd: EventCmd, x: Float = 2f, y: Float = 2f) = {
    val sprite = ResourceConstants.defaultSpriteSpec
    val states = Array(RpgEventState(sprite = Some(sprite), cmds = Array(cmd)))
    Map(
      1->RpgEvent(1, "Testevent", x, y, states)
    )
  }

  val tempDir = Files.createTempDir()
  val projectOption = rpgboss.util.ProjectCreator.create("test", tempDir)

  val project = projectOption.get

  val projectDirectory = tempDir
  val mapName = RpgMap.generateName(project.data.lastCreatedMapId)

  // Used to move assertions to the test thread
  val waiter = new Waiter
}

abstract class MapScreenTest extends ProjectTest with HasScriptConstants {
  def dismissWaiterAtEndOfTestScript = true

  def setup() = {
    val map = RpgMap.readFromDisk(project, mapName)
    val mapData = map.readMapData().get
    setupMapData(mapData)
    map.saveMapData(mapData) should be (true)
  }

  def setupMapData(mapData: RpgMapData) = {
    for (x <- 0 until 20; y <- 0 until 20)
      paintPassable(mapData.botLayer, x, y)
  }

  def testScript()

  val game = new RpgGame(projectDirectory) {
    override def beginGame() = {
      setup()

      startNewGame()

      setScreen(mapScreen)

      // Run this asynchronously so it doesn't block the main render thread.
      Future {
        testScript()

        if(dismissWaiterAtEndOfTestScript)
          waiter.dismiss()
      }
    }

    def setup() = MapScreenTest.this.setup()

    // Should only be run on scripting thread. |key| is an internal key.
    def mapScreenKeyPress(key: Int, duration: Float) = {
      GdxUtils.syncRun { mapScreen.inputs.myKeyDown(key) }
      Thread.sleep((duration * 1000).toLong)
      GdxUtils.syncRun { mapScreen.inputs.myKeyUp(key) }
    }

    def mapScreenKeyPress(key: Int): Unit = {
      mapScreenKeyPress(key, 0.1f)
    }
  }

  def scriptInterface = game.mapScreen.scriptInterface

  def runTest() = {
    val app = TestPlayer.launch(game)
    waiter.await(Timeout(Span(120, Seconds)))
  }
}