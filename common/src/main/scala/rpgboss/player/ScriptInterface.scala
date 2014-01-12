package rpgboss.player

import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.Gdx
import aurelienribon.tweenengine._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.Texture

case class EntityInfo(x: Float, y: Float, dir: Int)

object EntityInfo {
  def apply(e: Entity): EntityInfo = apply(e.x, e.y, e.dir)
}

// These methods should be called only from scripting threads. Calling these 
// methods on the Gdx threads will likely cause deadlocks.
class ScriptInterface(game: MyGame, mapLayer: MapLayer) {
  private def persistent = game.persistent
  private def project = game.project
  import game.syncRun
  
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
    mapLayer.playerEntity.setSprite(spritespec)
  }

  def setPlayerLoc(loc: MapLoc) = syncRun {
    mapLayer.playerEntity.x = loc.x
    mapLayer.playerEntity.y = loc.y
    mapLayer.playerEntity.mapName = Some(loc.map)
    
    mapLayer.updateMapAssets(if(loc.map.isEmpty) None else Some(loc.map)) 
  }
  
  def moveCamera(dx: Float, dy: Float, async: Boolean) = {
    val move = syncRun { mapLayer.camera.enqueueMove(dx, dy) }
    if (!async)
      move.awaitDone()
  }
  
  def getCameraPos() = syncRun {
    mapLayer.camera.info
  }

  /* 
   * Things to do with the screen
   */

  def setTransition(
    startAlpha: Float,
    endAlpha: Float,
    durationMs: Int) = syncRun {
    game.screenLayer.curTransition = 
      Some(Transition(startAlpha, endAlpha, durationMs))
  }

  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int, h: Int) =
    syncRun {
      game.screenLayer.pictures(slot).map(_.dispose())
      val picture = Picture.readFromDisk(project, name)
      game.screenLayer.pictures(slot) = 
        Some(PictureInfo(picture.newGdxTexture, x, y, w, h))
    }

  // TODO: Reconcile with showPicture
  def showTexture(slot: Int, texture: Texture, x: Int, y: Int, w: Int, h: Int) =
    syncRun {
      game.screenLayer.pictures(slot).map(_.dispose())
      game.screenLayer.pictures(slot) = 
        Some(PictureInfo(texture, x, y, w, h))
    }
  
  def hidePicture(slot: Int) = syncRun {
    game.screenLayer.pictures(slot).map(_.dispose())
    game.screenLayer.pictures(slot) = None
  }

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
    loop: Boolean, fadeDurationMs: Int) = syncRun {

    mapLayer.musics(slot).map({ oldMusic =>
      val tweenMusic = new GdxMusicTweenable(oldMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDurationMs / 1000f)
        .target(0f)
        .setCallback(new TweenCallback {
          override def onEvent(typeArg: Int, x: BaseTween[_]) = {
            if (typeArg == TweenCallback.COMPLETE) {
              oldMusic.stop()
            }
          }
        }).start(mapLayer.tweenManager)
    })

    mapLayer.musics(slot) = specOpt.map { spec =>
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
        .target(spec.volume).start(mapLayer.tweenManager)

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
    syncRun {
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

      game.screenLayer.windows.prepend(window)
      game.inputs.prepend(window)
      
      window
    }
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
  
  def getPlayerEntityInfo(): EntityInfo = syncRun {
    EntityInfo(mapLayer.playerEntity)
  }
  
  def getEventEntityInfo(id: Int): Option[EntityInfo] = {
    mapLayer.eventEntities.get(id).map(EntityInfo.apply)
  }
  
  def movePlayer(dx: Float, dy: Float,
                 affixDirection: Boolean = false, 
                 async: Boolean = false) = {
    moveEntity(mapLayer.playerEntity, dx, dy, affixDirection, async)
  }
  
  def activateEvent(id: Int, awaitFinish: Boolean) = {
    val eventOpt = mapLayer.eventEntities.get(id)
    val scriptOpt = eventOpt.flatMap(_.activate(SpriteSpec.Directions.NONE))
    
    if (awaitFinish)
      scriptOpt.map(_.awaitFinish())
    
    scriptOpt.isDefined
  }
  
  def moveEvent(id: Int, dx: Float, dy: Float,
                affixDirection: Boolean = false, 
                async: Boolean = false) = {
    val entityOpt = mapLayer.eventEntities.get(id)
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
        syncRun { entity.enqueueMove(EntityFaceDirection(direction)) }
      }
      
      val move = EntityMove(dx, dy)
      syncRun { entity.enqueueMove(move) }
      
      if (!async)
        move.awaitDone()
    }
  }

  def getInt(key: String): Int = syncRun {
    persistent.getInt(key)
  }
  def setInt(key: String, value: Int) = syncRun {
    persistent.setInt(key, value)
  }
  def getIntArray(key: String): Array[Int] = syncRun {
    persistent.getIntArray(key)
  }
  def setIntArray(key: String, value: Array[Int]) = syncRun {
    persistent.setIntArray(key, value)
  } 
  def getStringArray(key: String): Array[String] = syncRun {
    persistent.getStringArray(key)
  }
  def setStringArray(key: String, value: Array[String]) = syncRun {
    persistent.setStringArray(key, value)
  }
    
  def setNewGameVars() = {
    syncRun {
      mapLayer.playerEntity.setSprite(project.data.enums.characters.head.sprite)
    }
    // Initialize data structures
    setIntArray(PARTY, project.data.startup.startingParty.toArray);
    
    var characters = project.data.enums.characters.toArray;
    setStringArray(CHARACTER_NAMES, characters.map(_.name));
    
    setIntArray(CHARACTER_LEVELS, characters.map(_.initLevel))
    
    val characterStats = for (c <- characters)
      yield BattleStats(project.data, c.baseStats(project.data, c.initLevel), 
                        c.startingEquipment)
    
    setIntArray(CHARACTER_HPS, characterStats.map(_.mhp))
    setIntArray(CHARACTER_MPS, characterStats.map(_.mmp))
    setIntArray(CHARACTER_MAX_HPS, characterStats.map(_.mhp))
    setIntArray(CHARACTER_MAX_MPS, characterStats.map(_.mmp))
    
    setIntArray(CHARACTER_MAX_MPS, characters.map(x => 0))
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
  val CHARACTER_ROW = "characterRow"
    
  def CHARACTER_EQUIP(characterId: Int) = 
    "characterEquip-%d".format(characterId)
    
  def CHARACTER_STATUS_EFFECTS(characterId: Int) = 
    "characterStatusEffects-%d".format(characterId)
  
  val PICTURE_SLOTS = Constants.PictureSlots
}