package rpgboss.player

import com.google.common.io.Files
import java.awt._
import java.awt.image._
import java.io.File
import javax.imageio.ImageIO
import rpgboss._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.event._
import rpgboss.model.resource._
import rpgboss.player._
import rpgboss.player.entity.EntityMove

class MoveSpec extends UnitSpec {
  "Move" should "move right simple" in {
    val test = new MapScreenTest {
      override def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f);
        scriptInterface.movePlayer(1f, 0)

        val player = scriptInterface.getPlayerEntityInfo()

        waiter {
          val epsilon = 0.05f
          player.x should be (1.5f +- epsilon)
          player.y should be (0.5f +- epsilon)
        }
      }
    }

    test.runTest()
  }

  "Move" should "should work in reponse to key press" in {
    val test = new MapScreenTest {
      override def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f)

        // TODO: fix hardcoded speed here
        val speed = 4f
        scriptInterface.mapScreenKeyPress(MyKeys.Right, 5f / speed)

        val player = scriptInterface.getPlayerEntityInfo()

        waiter {
          val epsilon = 1.0f // slack because of sloppy key press simulation
          player.x should be (5.5f +- epsilon)
          player.y should be (0.5f +- epsilon)
        }
      }
    }

    test.runTest()
  }

  "MoveEvent" should "work with THIS_EVENT" in {
    val test = new MapScreenTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        mapData.events = singleTestEvent(
          MoveEvent(EntitySpec(WhichEntity.THIS_EVENT.id), 0f, 2f),
          2f,
          2f)
      }

      override def testScript() = {
        scriptInterface.teleport(mapName, 0.5f, 0.5f)
        scriptInterface.activateEvent(1, true)

        val entityInfo = scriptInterface.getEventEntityInfo(1)

        waiter {
          val epsilon = 0.05f

          entityInfo.x should be (2f +- epsilon)
          entityInfo.y should be (4f +- epsilon)
        }
      }
    }

    test.runTest()
  }
}
