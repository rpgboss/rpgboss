package rpgboss.player

import rpgboss._
import rpgboss.model.resource._
import rpgboss.player.entity._

class MapCameraSpec extends UnitSpec {
  "MapCamera" should "not go off edges or corners" in {
     val mapMetadata = RpgMapMetadata("", "", 50, 50)
     val camera = new MapCamera

     def updateCamera(delta: Float, trackedEntity: EntityLike,
                      forceSnapToEntity: Boolean) = {
       camera.update(delta, Some(trackedEntity), forceSnapToEntity, mapMetadata,
                     20, 15)
     }

     // Test snapping of camera.
     updateCamera(0, TestEntity(20, 20, 3), true)
     camera.x should equal (20)
     camera.y should equal (20)

     // Should track player but not move faster than player speed.
     updateCamera(2.0f, TestEntity(10, 20, 3), false)
     camera.x should equal (14)
     camera.y should equal (20)

     // Should track player perfectly if player moving at same speed as camera.
     updateCamera(2.0f, TestEntity(17, 20, 3), false)
     camera.x should equal (17)
     camera.y should equal (20)

     // Should not go off side in x axis.
     updateCamera(0, TestEntity(0, 20, 3), true)
     camera.x should equal (10)
     camera.y should equal (20)

     // Should not go off side in y axis
     updateCamera(0, TestEntity(20, 100, 3), true)
     camera.x should equal (20)
     camera.y should equal (50 - 15f / 2)

     // Should not go off corners
     updateCamera(0, TestEntity(100, 0, 3), true)
     camera.x should equal (50 - 20 / 2)
     camera.y should equal (15f / 2)
  }
}
