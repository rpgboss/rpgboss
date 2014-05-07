package rpgboss.player

import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.lib.ThreadChecked
import com.badlogic.gdx.graphics.g2d.TextureAtlas

case class PartyBattler(project: Project, spriteSpec: SpriteSpec, x: Int,
                        y: Int) {
  val spriteset = Spriteset.readFromDisk(project, spriteSpec.name)

  val imageW = spriteset.tileW.toFloat
  val imageH = spriteset.tileH.toFloat
}

/**
 * This class must be created and accessed only on the Gdx thread.
 * 
 * @param   gameOpt   Is optional to allow BattleScreen use in the editor.
 */
class BattleScreen(
  gameOpt: Option[RpgGame],
  assets: RpgAssetManager,
  atlasSprites: TextureAtlas,
  project: Project,
  screenW: Int,
  screenH: Int)
  extends ThreadChecked
  with RpgScreen {
  assume(atlasSprites != null)

  val scriptInterface = new ScriptInterface(gameOpt.orNull, this)

  val inputs = new InputMultiplexer()

  // Battle variables
  private var _battle: Option[Battle] = None
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]

  val windowManager = new WindowManager(assets, project, screenW, screenH)

  def battleActive = _battle.isDefined
  
  val enemyListWindow = {
    if (gameOpt.isDefined) {
      new TextWindow(
        gameOpt.get.persistent,
        windowManager,
        inputs,
        Array(),
        0, 300, 200, 180)
    } else {
      null
    }
  } 
  
  def updateEnemyListWindow() = {
    assert(_battle.isDefined)
    assert(enemyListWindow != null)
    val enemyLines = 
      Encounter.getEnemyLabels(_battle.get.encounter.units, project.data)
    enemyListWindow.updateText(enemyLines)
  }
  
  val partyListWindow = {
    if (gameOpt.isDefined) {
      new TextWindow(
        gameOpt.get.persistent,
        windowManager,
        inputs,
        Array(),
        200, 300, 440, 180)   
    } else {
      null
    }
  }
  
  def updatePartyListWindow() = {
    assert(_battle.isDefined)
    assert(partyListWindow != null)
    
    val partyLines = for (status <- _battle.get.partyStatus) yield {
      assert(status.id < _battle.get.pData.enums.characters.length)
      val name = _battle.get.pData.enums.characters(status.id).name
      val readiness = (math.min(status.readiness, 1.0) * 100).toInt
      "%-10s  %3d : %2d  %3d%%".format(name, status.hp, status.mp, readiness)
    }
    partyListWindow.updateText(partyLines)
  }
  
  def startBattle(battle: Battle, bgPicture: Option[String]) = {
    assert(onBoundThread())
    assert(_battle.isEmpty)

    bgPicture.map { picName =>
      // TODO: Make more robust
      windowManager.showPicture(PictureSlots.BATTLE_BACKGROUND, picName,
                                0, 0, 640, 320)
    }

    _battle = Some(battle)

    for ((unit, i) <- battle.encounter.units.zipWithIndex) {
      val enemy = project.data.enums.enemies(unit.enemyIdx)
      enemy.battler.map { battlerSpec =>
        val battler = Battler.readFromDisk(project, battlerSpec.name)
        val texture = battler.newGdxTexture

        val battlerWidth = (texture.getWidth() * battlerSpec.scale).toInt
        val battlerHeight = (texture.getHeight() * battlerSpec.scale).toInt

        windowManager.showTexture(
          PictureSlots.BATTLE_SPRITES_ENEMIES + i,
          texture,
          unit.x - battlerWidth / 2,
          unit.y - battlerHeight / 2,
          battlerWidth,
          battlerHeight)
      }
    }

    for ((partyId, i) <- battle.partyIds.zipWithIndex) {
      val character = project.data.enums.characters(partyId)
      character.sprite.map { spriteSpec =>
        val x = 10 * i + 550
        val y = 20 * i + 180
        _partyBattlers.append(PartyBattler(project, spriteSpec, x, y))
      }
    }
  }
  
  def endBattle() = {
    assert(onBoundThread())
    assert(_battle.isDefined)
    _battle = None

    for (i <- PictureSlots.BATTLE_BEGIN until PictureSlots.BATTLE_END) {
      windowManager.hidePicture(i)
    }

    _partyBattlers.clear()
  }

  def update(delta: Float) = {
    // TODO: Peel items off the ready queue. For a player, spawn a choice 
    // window. For an enemy, make a decision on the action.
    
    assert(onBoundThread())
    windowManager.update(delta)
  }

  def render() = {
    assert(onBoundThread())
    windowManager.render()

    windowManager.batch.begin()
    // TODO: Fix hack of re-using screenLayer's spritebatch
    for (partyBattler <- _partyBattlers) {
      GdxGraphicsUtils.renderSprite(
        windowManager.batch,
        atlasSprites,
        partyBattler.spriteset,
        partyBattler.spriteSpec.spriteIndex,
        SpriteSpec.Directions.WEST,
        SpriteSpec.Steps.STILL,
        partyBattler.x,
        partyBattler.y,
        partyBattler.imageW,
        partyBattler.imageH)
    }
    windowManager.batch.end()
  }

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    assert(onBoundThread())

    if (battleActive)
      endBattle()

    windowManager.dispose()
  }
}
