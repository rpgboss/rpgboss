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

/**
 * This class contains all the state information about the game.
 *
 * It must ensure that different threads can mutate the state of the game
 * without causing concurrency errors.
 *
 * Moreover, OpenGL operations may only occur on the GDX rendering thread.
 * This object must post those operations to that thread via postRunnable
 */
class GameState(game: MyGame, project: Project) {
  val tweenManager = new TweenManager()

  val musics = Array.fill[Option[GdxMusic]](8)(None)

  // Should only be modified on the Gdx thread
  var curTransition: Option[Transition] = None

  // current map
  var mapAndAssetsOption: Option[MapAndAssets] = None

  // protagonist. Modify all these things on the Gdx thread
  var playerEntity: PlayerEntity = new PlayerEntity(game)
  setPlayerSprite(game.project.data.enums.characters.head.sprite)

  val persistent = new PersistentState()

  // All the events on the current map, including the player event
  var eventEntities = List[EventEntity]()

  // Called every frame... by MyGame's render call. 
  def update(delta: Float) = {
    // Update tweens
    tweenManager.update(delta)

    // Update events, including player event
    eventEntities.foreach(_.update(delta))
    playerEntity.update(delta)

    // Update current transition
    curTransition.synchronized {
      curTransition map { transition =>
        if (transition.done) {
          curTransition = None
        }
      }
    }

    // Update camera location
    if (playerEntity.isMoving()) {
      persistent.cameraLoc.x = playerEntity.x
      persistent.cameraLoc.y = playerEntity.y
    }
  }

  import game.syncRun

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    mapAndAssetsOption.map(_.dispose())
  }

  /*
   * The below functions are all called from the script threads only.
   */

  /*
   * Accessors to various game data
   */
  def getMap(loc: MapLoc) =
    RpgMap.readFromDisk(project, loc.map)

  /*
   * Things to do with the player's location and camera
   */

  def setPlayerSprite(spritespec: Option[SpriteSpec]) = syncRun {
    playerEntity.setSprite(spritespec)
  }

  def setPlayerLoc(loc: MapLoc) = syncRun {
    playerEntity.x = loc.x
    playerEntity.y = loc.y
    setCameraLoc(loc)
  }

  def setCameraLoc(loc: MapLoc) = syncRun {
    persistent.cameraLoc.set(loc)

    // Update internal resources for the map
    if (persistent.cameraLoc.map.isEmpty()) {
      mapAndAssetsOption.map(_.dispose())
      mapAndAssetsOption = None
      eventEntities = List()
    } else {
      mapAndAssetsOption.map(_.dispose())

      val mapAndAssets = new MapAndAssets(project, loc.map)
      mapAndAssetsOption = Some(mapAndAssets)
      eventEntities = mapAndAssets.mapData.nonDeletedEvents.map {
        new EventEntity(game, _)
      }.toList
    }
  }

  /* 
   * Things to do with the screen
   */

  def setTransition(
    startAlpha: Float,
    endAlpha: Float,
    durationMs: Int) = syncRun {
    curTransition = Some(Transition(startAlpha, endAlpha, durationMs))
  }

  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int, h: Int) =
    syncRun {
      persistent.pictures(slot).map(_.dispose())
      persistent.pictures(slot) = Some(PictureInfo(project, name, x, y, w, h))
    }

  def hidePicture(slot: Int) = syncRun {
    persistent.pictures(slot).map(_.dispose())
    persistent.pictures(slot) = None
  }

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
    loop: Boolean, fadeDurationMs: Int) = syncRun {

    musics(slot).map({ oldMusic =>
      val tweenMusic = new GdxMusicTweenable(oldMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDurationMs / 1000f)
        .target(0f)
        .setCallback(new TweenCallback {
          override def onEvent(typeArg: Int, x: BaseTween[_]) = {
            if (typeArg == TweenCallback.COMPLETE) {
              oldMusic.stop()
            }
          }
        }).start(tweenManager)
    })

    musics(slot) = specOpt.map { spec =>
      val resource = Music.readFromDisk(project, spec.sound)
      resource.loadAsset(game.assets)
      // TODO: fix this blocking call
      game.assets.finishLoading()
      val newMusic = resource.getAsset(game.assets)

      // Start at zero volume and fade to desired volume
      newMusic.stop()
      newMusic.setVolume(0f)
      newMusic.setLooping(loop)
      newMusic.play()

      // Setup volume tween
      val tweenMusic = new GdxMusicTweenable(newMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDurationMs / 1000f)
        .target(spec.volume).start(tweenManager)

      newMusic
    }
  }

  /*
   * Things to do with user interaction
   */
  def sleep(durationMs: Int) = {
    Thread.sleep(durationMs)
  }

  def newChoiceWindow(
    lines: Array[String],
    x: Int, y: Int, w: Int, h: Int,
    justification: Int,
    columns: Int,
    displayedLines: Int,
    allowCancel: Boolean): ChoiceWindow = {
    val window = new ChoiceWindow(
      game.screenLayer.getWindowId(),
      game,
      game.assets,
      project,
      lines,
      x, y, w, h,
      game.screenLayer.windowskin,
      game.screenLayer.windowskinRegion,
      game.screenLayer.fontbmp,
      initialState = Window.Opening,
      justification = justification,
      columns = columns,
      displayedLines = displayedLines,
      allowCancel = allowCancel)

    syncRun {
      game.screenLayer.windows.prepend(window)
      game.inputs.prepend(window)
    }

    window
  }

  def newChoiceWindow(
    choices: Array[String],
    x: Int, y: Int, w: Int, h: Int): ChoiceWindow =
    newChoiceWindow(choices, x, y, w, h,
      Window.Left,
      1 /* columns */ ,
      0 /* displayedChoices */ ,
      false /* allowCancel */ )

  def newTextWindow(text: Array[String], x: Int, y: Int, w: Int, h: Int,
                    msPerChar: Int) = {
    val window = new PrintingTextWindow(
      game.screenLayer.getWindowId(),
      game,
      game.assets,
      project,
      text,
      x, y, w, h,
      game.screenLayer.windowskin,
      game.screenLayer.windowskinRegion,
      game.screenLayer.fontbmp,
      msPerChar)

    syncRun {
      game.screenLayer.windows.prepend(window)
      game.inputs.prepend(window)
    }

    window
  }
  
  def getEvtState(evtName: String): Int =
    getEvtState(persistent.cameraLoc.map, evtName)
  def getEvtState(mapName: String, evtName: String) =
    persistent.getEventState(mapName, evtName)
  def setEvtState(evtName: String, newState: Int): Unit =
    setEvtState(persistent.cameraLoc.map, evtName, newState)
  def setEvtState(mapName: String, evtName: String, newState: Int) = {
    persistent.setEventState(mapName, evtName, newState)

    if (mapName == persistent.cameraLoc.map) {
      eventEntities.filter(_.mapEvent.name == evtName).foreach(_.updateState())
    }
  }
  
  def getPlayerEntity() = playerEntity
  def getEventEntity(evtName: String): EventEntity = {
    eventEntities.find(_.mapEvent.name == evtName).getOrElse(null)
  }
  
  def moveEntity(entity: Entity, dx: Float, dy: Float,
                 affixDirection: Boolean, async: Boolean) {
    import SpriteSpec.Directions._
    if (dx == 0 && dy == 0)
      return
      
    if (!affixDirection) {
      val direction = 
        if (math.abs(dx) > math.abs(dy))
          if (dx > 0) EAST else WEST
        else
          if (dy > 0) SOUTH else NORTH
      entity.enqueueMove(EntityFaceDirection(direction))
    }
    
    val move = EntityMove(dx, dy)
    entity.enqueueMove(move)
    
    if (!async)
      move.awaitDone()
  }

  def getInt(key: String): Int = persistent.getInt(key)
  def setInt(key: String, value: Int) = {
    persistent.setInt(key, value)
    eventEntities.foreach(_.updateState())
  }

  def getIntArray(key: String): Array[Int] = persistent.getIntArray(key)
  def setIntArray(key: String, value: Array[Int]) =
    persistent.setIntArray(key, value)

  def getStringArray(key: String): Array[String] = 
    persistent.getStringArray(key)
  def setStringArray(key: String, value: Array[String]) =
    persistent.setStringArray(key, value)
    
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right

  val PARTY = "party"
  val INVENTORY_IDXS = "inventoryIdxs"
  val INVENTORY_QTYS = "inventoryQtys"
  val CHARACTER_NAMES = "characterNames"
  val CHARACTER_LEVELS = "characterLevels"
  val CHARACTER_HPS = "characterHps"
  val CHARACTER_MPS = "characterMps"
  val CHARACTER_MAX_HPS = "characterMaxHps"
  val CHARACTER_MAX_MPS = "characterMaxMps"
}

/**
 * Need call on dispose first
 */
case class PictureInfo(
  project: Project,
  name: String,
  x: Int, y: Int, w: Int, h: Int) {
  val picture = Picture.readFromDisk(project, name)
  val texture =
    new Texture(Gdx.files.absolute(picture.dataFile.getAbsolutePath()))

  def dispose() = texture.dispose()

  def render(batch: SpriteBatch) = {
    batch.draw(texture,
      x, y, w, h,
      0, 0, texture.getWidth(), texture.getHeight(),
      false, true)
  }
}