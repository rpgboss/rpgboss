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
    val test = new BasicTest {
      def testScript() = {
        scriptInterface.setPlayerLoc(MapLoc(mapName, 0.5f, 0.5f));
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
  
  "MoveEvent" should "work with player" in {
    val test = new BasicTest {
      override def setupMapData(mapData: RpgMapData) = {
        super.setupMapData(mapData)
        val sprite = SpriteSpec("vx_chara02_a.png", 0)
        val cmds: Array[EventCmd] = Array(MoveEvent(
          EntitySpec(WhichEntity.THIS_EVENT.id),
          0f, 2f))
        val states = Array(RpgEventState(sprite = Some(sprite), cmds = cmds))
        mapData.events = Map(
          1->RpgEvent(1, "Testevent", 2f, 2f, states)
        )
      }
      
      def testScript() = {
        scriptInterface.setPlayerLoc(MapLoc(mapName, 0.5f, 0.5f));
        scriptInterface.activateEvent(1, true)
        
        val entityInfoOpt = scriptInterface.getEventEntityInfo(1)
        
        waiter {
          val epsilon = 0.05f
          
          entityInfoOpt.isDefined should equal (true)
          
          entityInfoOpt map { e =>
            e.x should be (2f +- epsilon)
            e.y should be (4f +- epsilon)
          }
        }
      }
    }
    
    test.runTest()
  }
}