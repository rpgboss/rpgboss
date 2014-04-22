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

  val windowManager = new WindowManager(project, screenW, screenH)

  def battleActive = _battle.isDefined

  def startBattle(battle: Battle, bgPicture: Option[String]) = {
    assert(onValidThread())
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
    
    for (game <- gameOpt) {
      val enemyLines = Array("Enemy1", "Enemy2")
      // TODO: Eliminate these literal numbers for dimensions.
      val enemyListWindow = new TextWindow(
        game.persistent,
        windowManager,
        inputs,
        game.assets,
        project,
        enemyLines,
        0, 300, 200, 180,
        windowManager.windowskin,
        windowManager.windowskinRegion,
        windowManager.fontbmp,
        initialState = Window.Open)
      windowManager.addWindow(enemyListWindow)
    }
  }

  def endBattle() = {
    assert(onValidThread())
    assert(_battle.isDefined)
    _battle = None

    for (i <- PictureSlots.BATTLE_BEGIN until PictureSlots.BATTLE_END) {
      windowManager.hidePicture(i)
    }

    _partyBattlers.clear()
  }

  def update(delta: Float) = {
    assert(onValidThread())
    windowManager.update(delta)
  }

  def render() = {
    assert(onValidThread())
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
    assert(onValidThread())

    if (battleActive)
      endBattle()

    windowManager.dispose()
  }
}
