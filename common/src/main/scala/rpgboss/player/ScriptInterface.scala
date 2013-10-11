package rpgboss.player

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.Gdx
import aurelienribon.tweenengine._
import rpgboss.player.entity._

case class EntityInfo(x: Float, y: Float, dir: Int)

object EntityInfo {
  def apply(e: Entity): EntityInfo = apply(e.x, e.y, e.dir)
}

// These methods should be called only from scripting threads. Calling these 
// methods on the Gdx threads will likely cause deadlocks.
class ScriptInterface(game: MyGame, state: GameState) {
  private def persistent = state.persistent
  private def project = game.project
  private def syncRun(op: => Any) = game.syncRun(op)
  
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
    state.playerEntity.setSprite(spritespec)
  }

  def setPlayerLoc(loc: MapLoc) = syncRun {
    state.playerEntity.x = loc.x
    state.playerEntity.y = loc.y
    
    persistent.cameraLoc.set(loc)
    state.updateMapAssets(if(loc.map.isEmpty) None else Some(loc.map)) 
  }

  /* 
   * Things to do with the screen
   */

  def setTransition(
    startAlpha: Float,
    endAlpha: Float,
    durationMs: Int) = syncRun {
    state.curTransition = Some(Transition(startAlpha, endAlpha, durationMs))
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

    state.musics(slot).map({ oldMusic =>
      val tweenMusic = new GdxMusicTweenable(oldMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDurationMs / 1000f)
        .target(0f)
        .setCallback(new TweenCallback {
          override def onEvent(typeArg: Int, x: BaseTween[_]) = {
            if (typeArg == TweenCallback.COMPLETE) {
              oldMusic.stop()
            }
          }
        }).start(state.tweenManager)
    })

    state.musics(slot) = specOpt.map { spec =>
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
        .target(spec.volume).start(state.tweenManager)

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
  
  def getPlayerEntityInfo() = syncRun {
    EntityInfo(state.playerEntity)
  }
  
  def getEventEntityInfo(id: Int): Option[EntityInfo] = {
    state.eventEntities.get(id).map(EntityInfo.apply)
  }
  
  def movePlayer(dx: Float, dy: Float,
                 affixDirection: Boolean = false, 
                 async: Boolean = false) = {
    moveEntity(state.playerEntity, dx, dy, affixDirection, async)
  }
  
  def activateEvent(id: Int, awaitFinish: Boolean) = {
    val eventOpt = state.eventEntities.get(id)
    val scriptOpt = eventOpt.flatMap(_.activate(SpriteSpec.Directions.NONE))
    
    if (awaitFinish)
      scriptOpt.map(_.awaitFinish())
    
    scriptOpt.isDefined
  }
  
  def moveEvent(id: Int, dx: Float, dy: Float,
                affixDirection: Boolean = false, 
                async: Boolean = false) = {
    val entityOpt = state.eventEntities.get(id)
    entityOpt.foreach { entity => 
      moveEntity(entity, dx, dy, affixDirection, async)
    }
    entityOpt.isDefined
  }
    
  private def moveEntity(entity: Entity, dx: Float, dy: Float,
                         affixDirection: Boolean = false, 
                         async: Boolean = false) = {
    import SpriteSpec.Directions._
    if (dx != 0 || dy != 0) {
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
  }

  def getInt(key: String): Int = state.persistent.getInt(key)
  // This must be synchronized because this updates event states
  def setInt(key: String, value: Int) = syncRun {
    state.setInt(key, value)
  }

  def getIntArray(key: String): Array[Int] = persistent.getIntArray(key)
  def setIntArray(key: String, value: Array[Int]) =
    persistent.setIntArray(key, value)

  def getStringArray(key: String): Array[String] = 
    persistent.getStringArray(key)
  def setStringArray(key: String, value: Array[String]) =
    persistent.setStringArray(key, value)
    
  def setNewGameVars() = {
    syncRun {
      state.playerEntity.setSprite(
          game.project.data.enums.characters.head.sprite)
    }
    // Initialize data structures
    setIntArray(PARTY, project.data.startup.startingParty.toArray);
    
    var characters = project.data.enums.characters.toArray;
    setStringArray(CHARACTER_NAMES, characters.map(_.name));
    
    setIntArray(CHARACTER_LEVELS, characters.map(_.initLevel))
    setIntArray(CHARACTER_HPS, characters.map(_.initMhp))
    setIntArray(CHARACTER_MPS, characters.map(_.initMmp))
    setIntArray(CHARACTER_MAX_HPS, characters.map(_.initMhp))
    setIntArray(CHARACTER_MAX_MPS, characters.map(_.initMmp))
  }
    
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