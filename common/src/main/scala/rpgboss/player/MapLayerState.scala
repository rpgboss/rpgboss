package rpgboss.player
import rpgboss.player.entity._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import com.badlogic.gdx.audio.{ Music => GdxMusic }
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import rpgboss.player.entity.PlayerEntity
import rpgboss.player.entity.EventEntity
import rpgboss.player.entity.Entity
import aurelienribon.tweenengine._
import rpgboss.model.battle._

/** This class contains all the state information about the game.
 *  
 *  All these methods should be called on the Gdx thread only.
 */
class MapLayerState(game: MyGame, project: Project) {
  val tweenManager = new TweenManager()

  val musics = Array.fill[Option[GdxMusic]](8)(None)

  // Should only be modified on the Gdx thread
  var curTransition: Option[Transition] = None

  // current map
  var mapAndAssetsOption: Option[MapAndAssets] = None
  def mapName = mapAndAssetsOption.map(_.map.name)

  // protagonist. Modify all these things on the Gdx thread
  var playerEntity: PlayerEntity = new PlayerEntity(game)
  
  val camera = new MapCamera(game)
  
  // All the events on the current map, including the player event
  var eventEntities = Map[Int, EventEntity]()
  
  def updateMapAssets(mapNameOption: Option[String]) = {
    if (mapNameOption.isDefined) {
      val mapName = mapNameOption.get
      mapAndAssetsOption.map(_.dispose())

      val mapAndAssets = new MapAndAssets(project, mapNameOption.get)
      mapAndAssetsOption = Some(mapAndAssets)
      eventEntities = mapAndAssets.mapData.events.map {
        case (k, v) => ((k, new EventEntity(game, mapName, v)))
      }
    } else {
      mapAndAssetsOption.map(_.dispose())
      mapAndAssetsOption = None
      eventEntities = Map.empty
    }
  }
      
  // Called every frame... by MyGame's render call. 
  def update(delta: Float) = {
    // Update tweens
    tweenManager.update(delta)

    // Update events, including player event
    eventEntities.values.foreach(_.update(delta))
    playerEntity.update(delta)

    camera.update(delta)
  }

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    mapAndAssetsOption.map(_.dispose())
  }
}

/**
 * Need call on dispose first
 */
case class PictureInfo(
  texture: Texture,
  x: Int, y: Int, w: Int, h: Int) {

  def dispose() = texture.dispose()

  def render(batch: SpriteBatch) = {
    batch.draw(texture,
      x, y, w, h,
      0, 0, texture.getWidth(), texture.getHeight(),
      false, true)
  }
}