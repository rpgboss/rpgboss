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

class BattleState(project: Project, screenW: Int, screenH: Int) 
  extends ThreadChecked {
  // Battle variables
  private var _battle: Option[Battle] = None
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]

  val screenLayer = new ScreenLayer(project, screenW, screenH)

  def battleActive = _battle.isDefined

  def startBattle(battle: Battle, bgPicture: Option[String]) = {
    assert(onValidThread())
    assert(_battle.isEmpty)

    bgPicture.map { picName =>
      // TODO: Make more robust
      screenLayer.showPicture(PictureSlots.BATTLE_BACKGROUND, picName, 
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
        
        screenLayer.showTexture(
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
    assert(onValidThread())
    assert(_battle.isDefined)
    _battle = None

    for (i <- PictureSlots.BATTLE_BEGIN until PictureSlots.BATTLE_END) {
      screenLayer.hidePicture(i)
    }

    _partyBattlers.clear()
  }

  def update(delta: Float) = {
    assert(onValidThread())
    screenLayer.update(delta)
  }

  def render(atlasSprites: TextureAtlas) = {
    assert(onValidThread())
    screenLayer.render()
    
    screenLayer.batch.begin()
    // TODO: Fix hack of re-using screenLayer's spritebatch
    for (partyBattler <- _partyBattlers) {
      GdxGraphicsUtils.renderSprite(
        screenLayer.batch, 
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
    screenLayer.batch.end()
  }

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    assert(onValidThread())
    
    if (battleActive)
      endBattle()
    
    screenLayer.dispose()
  }
}
