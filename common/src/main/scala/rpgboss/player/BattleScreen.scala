package rpgboss.player

import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Logger
import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.lib._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Channel

case class PartyBattler(project: Project, spriteSpec: SpriteSpec, x: Float,
                        y: Float) extends BoxLike {
  val spriteset = Spriteset.readFromDisk(project, spriteSpec.name)

  val w = spriteset.tileW.toFloat
  val h = spriteset.tileH.toFloat
}

/**
 * This class must be created and accessed only on the Gdx thread.
 *
 * @param   gameOpt   Is optional to allow BattleScreen use in the editor.
 */
class BattleScreen(
  gameOpt: Option[RpgGame],
  val assets: RpgAssetManager,
  atlasSprites: TextureAtlas,
  val project: Project,
  val screenW: Int,
  val screenH: Int)
  extends ThreadChecked
  with RpgScreen
  with HasScriptConstants {
  assume(atlasSprites != null)

  val scriptInterface = gameOpt.map(new ScriptInterface(_, this)).orNull

  val logger = new Logger("BatleScreen", Logger.INFO)

  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, screenW, screenH) // y points down
  screenCamera.update()

  val batch = new SpriteBatch()

  batch.setProjectionMatrix(screenCamera.combined)
  batch.enableBlending()
  batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

  /**
   * Read this channel to await a battle being finished.
   */
  val finishChannel = new Channel[Int]()

  override def createWindowManager() =
    new WindowManager(assets, project, screenW, screenH)

  object PlayerActionWindow extends ThreadChecked {
    import concurrent.ExecutionContext.Implicits.global

    class RunningWindow(battle: Battle, val actor: BattleStatus) {

      assertOnBoundThread()
      assert(battle != null)

      private var _window: ChoiceWindow#ChoiceWindowScriptInterface = null

      def close() = {
        assertOnBoundThread()
        concurrent.Future {
          _window.close()
        }
      }

      /**
       * Returns true when we are done and the action window should exit.
       * Otherwise, it's expected to get another choice and try again.
       */
      def processWindow(choice: Int): Boolean = {
        assertOnDifferentThread()

        choice match {
          case 0 => {
            val targets = getTargets(Scope.OneEnemy)

            if (targets.isEmpty)
              return false

            GdxUtils.syncRun {
              _battle.get.takeAction(AttackAction(actor, targets.get))
            }
            return true
          }
          case 1 => {
            val skillChoices = for (skillId <- actor.knownSkillIds) yield {
              assert(skillId < battle.pData.enums.skills.length)
              battle.pData.enums.skills(skillId)
            }

            val skillWindow = scriptInterface.newChoiceWindow(
              skillChoices.map(_.name), Layout(SOUTH, FIXED, 640, 180),
              TextChoiceWindowOptions(columns = 2, allowCancel = true))

            while (true) {
              val idxInChoiceBox = skillWindow.getChoice()
              if (idxInChoiceBox == -1 ||
                  idxInChoiceBox >= actor.knownSkillIds.length) {
                skillWindow.close()
                return false
              }

              val skillId = actor.knownSkillIds(idxInChoiceBox)
              val skill = skillChoices(idxInChoiceBox)
              val scope = Scope(skill.scopeId)
              val targets = getTargets(scope)
              if (targets.isDefined) {
                skillWindow.close()
                GdxUtils.syncRun {
                  _battle.get.takeAction(
                    SkillAction(actor, targets.get, skillId))
                }
                return true
              }
            }

            assert(0 == 1) // We should never reach this point
            skillWindow.close()
            return true
          }
          case 2 => {
            assert(gameOpt.isDefined)
            val game = gameOpt.get

            val battleItems = GdxUtils.syncRun {
              game.persistent.getBattleItems(battle.pData)
            }

            val battleItemInstances =
              battleItems.map(x => battle.pData.enums.items(x._1))

            val window = scriptInterface.newChoiceWindow(
              battleItemInstances.map(_.name), Layout(SOUTH, FIXED, 640, 180),
              TextChoiceWindowOptions(columns = 2, allowCancel = true))

            while (true) {
              val idxInChoiceBox = window.getChoice()
              if (idxInChoiceBox == -1 ||
                  idxInChoiceBox >= battleItems.length) {
                window.close()
                return false
              }

              val itemId = battleItems(idxInChoiceBox)._1
              val item = battleItemInstances(idxInChoiceBox)
              val skillId = item.onUseSkillId
              val skill = battle.pData.enums.skills(idxInChoiceBox)
              val scope = Scope(skill.scopeId)
              val targets = getTargets(scope)
              if (targets.isDefined) {
                window.close()
                GdxUtils.syncRun {
                  _battle.get.takeAction(
                    ItemAction(actor, targets.get, itemId, game.persistent))
                }
                return true
              }
            }

            assert(0 == 1) // We should never reach this point
            window.close()
            return true
          }
          case _ => {
            GdxUtils.syncRun {
              // This is optional because this condition may be reached
              // if the battle is over and the outstanding window closes.
              _battle.map(_.takeAction(NullAction(actor)))
            }
            return true
          }
        }
      }

      def run() = concurrent.Future {
        assertOnDifferentThread()
        PlayerActionWindow.synchronized {
          currentOpt = Some(this)

          _window = scriptInterface.newChoiceWindow(
            Array("Attack", "Skill", "Item"), Layout(SOUTH, FIXED, 140, 180),
            TextChoiceWindowOptions(allowCancel = true))
        }

        var done = false
        while (!done) {
          done = processWindow(_window.getChoice())
        }

        _window.close()

        PlayerActionWindow.synchronized {
          currentOpt = None
        }
      }

      /**
       * Makes the player choose the targets of the action based on the scope.
       */
      def getTargets(scope: Scope.Value): Option[Array[BattleStatus]] = {
        assertOnDifferentThread()
        assert(_enemyBattlers.length == _battle.get.enemyStatus.length)
        assert(_partyBattlers.length == _battle.get.partyStatus.length)

        val enemyList = _battle.get.enemyStatus
        val partyList = _battle.get.partyStatus

        val (aliveEnemyList, deadEnemyList) = enemyList.partition(_.alive)
        val (alivePartyList, deadPartyList) = partyList.partition(_.alive)

        val choices = new ArrayBuffer[Array[BattleStatus]]
        scope match {
          case Scope.AllAllies =>
            choices.append(partyList)
            choices.append(aliveEnemyList)
          case Scope.AllAlliesDead =>
            choices.append(deadPartyList)
          case Scope.AllEnemies =>
            choices.append(aliveEnemyList)
            choices.append(alivePartyList)
          case Scope.OneAlly =>
            alivePartyList.map(x => choices.append(Array(x)))
            aliveEnemyList.map(x => choices.append(Array(x)))
          case Scope.OneAllyDead =>
            deadPartyList.map(x => choices.append(Array(x)))
            deadEnemyList.map(x => choices.append(Array(x)))
          case Scope.OneEnemy =>
            aliveEnemyList.map(x => choices.append(Array(x)))
            alivePartyList.map(x => choices.append(Array(x)))
          case Scope.SelfOnly =>
            choices.append(Array(actor))
        }

        def toBattler(status: BattleStatus) = status.entityType match {
          case BattleEntityType.Enemy => _enemyBattlers(status.index)
          case BattleEntityType.Party => _partyBattlers(status.index)
        }

        // Convert each choice to a Set[Rect]
        val boxChoices: Array[Set[Rect]] = choices.map { choice =>
          choice.map(x => toBattler(x).getRect()).toSet
        }.toArray

        val choiceIdx = scriptInterface.getSpatialChoice(
          boxChoices, defaultChoice = 0)

        if (choiceIdx == -1)
          None
        else
          Some(choices(choiceIdx))
      }
    }

    // Accessed on multiple threads
    var currentOpt: Option[RunningWindow] = None

    def closeCurrentIfDead() = synchronized {
      assertOnBoundThread()

      // Closing needs to execute on script thread.
      currentOpt.map(current => {
        if (current.actor.hp <= 0) {
          // Closing will write a -1 to the choiceChannel and cause a NullAction
          // to be processed for this entity.
          current.close()
        }
      })
    }

    def spawnIfNeeded(battle: Battle,
                      readyEntity: Option[BattleStatus]) = synchronized {
      assertOnBoundThread()

      if (readyEntity.isDefined && currentOpt.isEmpty &&
          readyEntity.get.entityType == BattleEntityType.Party) {
        val window = new RunningWindow(_battle.get, readyEntity.get)
        window.run()
      }
    }
  }

  // Battle variables
  private var _battle: Option[Battle] = None
  private val _enemyBattlers = new collection.mutable.ArrayBuffer[Rect]
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]

  // How long to wait after being defeated.
  private var _defeatedTimer = 0.0f
  private def defeatedMessageTime = 4.0f
  private var _victorySequenceStarted = false

  private var enemyListWindow: PrintingTextWindow = null
  private var partyListWindow: PrintingTextWindow = null

  /**
   * Contains the state of the currently displaying battle notification.
   * This comprises of damage text, animations, etc.
   */
  case class NotificationDisplay(
    notification: BattleActionNotification,
    windows: Seq[Window],
    animations: Seq[AnimationPlayer]) {
    def done =
      windows.forall(_.state == Window.Closed) &&
      animations.forall(_.visualsDone)
  }
  var currentNotificationDisplay: Option[NotificationDisplay] = None

  def getBox(entityType: BattleEntityType.Value, id: Int): BoxLike = {
    assume(entityType == BattleEntityType.Party ||
        entityType == BattleEntityType.Enemy)

    if (entityType == BattleEntityType.Party) {
      return _partyBattlers(id)
    } else {
      return _enemyBattlers(id)
    }
  }

  def battleActive = _battle.isDefined

  val persistentState = gameOpt.map(_.persistent).getOrElse(new PersistentState)

  def getCharacterName(characterId: Int) = {
    assertOnBoundThread()
    assume(battleActive)
    val names =
      persistentState.getStringArray(ScriptInterfaceConstants.CHARACTER_NAMES)
    if (characterId < names.length) {
      names(characterId)
    } else {
      assert(characterId < _battle.get.pData.enums.characters.length)
      _battle.get.pData.enums.characters(characterId).name
    }
  }

  def getEntityName(status: BattleStatus) = {
    assume(battleActive)
    if (status.entityType == BattleEntityType.Party) {
      getCharacterName(status.entityId)
    } else {
      _battle.get.pData.enums.enemies(status.entityId).name
    }
  }

  def updateEnemyListWindowAndBattlers() = {
    assert(_battle.isDefined)
    assert(enemyListWindow != null)
    val aliveUnits = _battle.get.encounter.units.zipWithIndex.filter {
      case (_, i) => _battle.get.enemyStatus.apply(i).alive
    }.map(_._1)

    val enemyLines =
      Encounter.getEnemyLabels(aliveUnits, project.data)
    enemyListWindow.updateLines(enemyLines)

    // TODO: Handle enemy revive
    for (enemyStatus <- _battle.get.enemyStatus; if !enemyStatus.alive) {
      windowManager.hidePicture(
        PictureSlots.BATTLE_SPRITES_ENEMIES + enemyStatus.index)
    }
  }

  def updatePartyListWindowAndBattlers() = {
    assert(_battle.isDefined)
    assert(partyListWindow != null)

    val partyLines = for (status <- _battle.get.partyStatus) yield {
      assert(status.entityId < _battle.get.pData.enums.characters.length)
      val name = getCharacterName(status.entityId)
      val readiness = (math.min(status.readiness, 1.0) * 100).toInt
      "%-10s %3dHP | %2dMP %3d%% ".format(name, status.hp, status.mp, readiness)
    }
    partyListWindow.updateLines(partyLines)

    // TODO: Handle party revive
    for (status <- _battle.get.partyStatus; if !status.alive) {
      windowManager.hidePicture(
        PictureSlots.BATTLE_SPRITES_PARTY + status.index)
    }
  }

  def startBattle(battle: Battle, battleBackground: String) = {
    assertOnBoundThread()
    assert(_battle.isEmpty)

    _battle = Some(battle)

    if (gameOpt.isDefined) {
      enemyListWindow = {
        new PrintingTextWindow(
          persistentState,
          windowManager,
          null,
          Array(),
          Layout(SOUTHWEST, FIXED, 200, 180),
          PrintingTextWindowOptions(timePerChar = 0)) {
          override def openCloseTime = 0
        }
      }

      partyListWindow = {
        new PrintingTextWindow(
          persistentState,
          windowManager,
          null,
          Array(),
          Layout(SOUTHEAST, FIXED, 440, 180),
          PrintingTextWindowOptions(timePerChar = 0)) {
          override def openCloseTime = 0
        }
      }
    }

    if (!battleBackground.isEmpty) {
      val bg = BattleBackground.readFromDisk(project, battleBackground)
      windowManager.showPicture(
        PictureSlots.BATTLE_BACKGROUND,
        TexturePicture(assets, bg, Layout(NORTH, COVER, 0, 0)))
    }

    assert(_enemyBattlers.isEmpty)
    for ((unit, i) <- battle.encounter.units.zipWithIndex) {
      val enemy = project.data.enums.enemies(unit.enemyIdx)
      enemy.battler.map { battlerSpec =>
        val battler = Battler.readFromDisk(project, battlerSpec.name)

        val battlerWidth = (battler.img.getWidth() * battlerSpec.scale).toInt
        val battlerHeight = (battler.img.getHeight() * battlerSpec.scale).toInt

        val layout =
          Layout(NORTHWEST, FIXED, battlerWidth, battlerHeight, unit.x, unit.y)
        val rect = layout.getRect(unit.x, unit.y, screenW, screenH)
        windowManager.showPicture(
          PictureSlots.BATTLE_SPRITES_ENEMIES + i,
          TexturePicture(
            assets,
            battler,
            layout))

        _enemyBattlers.append(rect)
      }
    }

    assert(_partyBattlers.isEmpty)
    for ((partyId, i) <- battle.partyIds.zipWithIndex) {
      val character = project.data.enums.characters(partyId)
      character.sprite.map { spriteSpec =>
        val x = 10 * i + 550
        val y = 20 * i + 180
        val battler = PartyBattler(project, spriteSpec, x, y)

        val (srcX, srcY) = battler.spriteset.srcTexels(
          spriteSpec.spriteIndex,
          SpriteSpec.Directions.WEST,
          SpriteSpec.Steps.STEP0)

        windowManager.showPicture(
          PictureSlots.BATTLE_SPRITES_PARTY + i,
          TextureAtlasRegionPicture(
            atlasSprites,
            battler.spriteset.name,
            battler.x,
            battler.y,
            battler.w,
            battler.h,
            srcX,
            srcY,
            battler.spriteset.tileW,
            battler.spriteset.tileH))

        _partyBattlers.append(battler)
      }
    }
  }

  def endBattle() = {
    assertOnBoundThread()
    assert(_battle.isDefined)
    _battle = None
    _defeatedTimer = 0
    _victorySequenceStarted = false

    currentNotificationDisplay = None

    enemyListWindow = null
    partyListWindow = null

    reset()

    _enemyBattlers.clear()
    _partyBattlers.clear()

    finishChannel.write(0)
  }

  def postTextNotice(msg: String) = {
    new PrintingTextWindow(gameOpt.get.persistent, windowManager, null,
        Array(msg), Layout(NORTH, FIXED, 640, 60),
        PrintingTextWindowOptions(timePerChar = 0.01f, stayOpenTime = 1.0f))
  }

  def update(delta: Float): Unit = {
    // All these actions should not take place if this is an in-editor session.
    gameOpt map { game =>
      _battle.map { battle =>
        // Handle defeat
        if (battle.defeat) {
          if (_defeatedTimer == 0) {
            postTextNotice("Defeat...")
          }
          _defeatedTimer += delta
          if (_defeatedTimer >= defeatedMessageTime) {
            endBattle()
            game.gameOver()
          }
        } else if (battle.victory) {
          if (!_victorySequenceStarted) {
            _victorySequenceStarted = true

            // Save party vitals
            battle.partyStatus.map { status =>
              game.persistent.saveCharacterVitals(
                  status.entityId,
                  status.hp,
                  status.mp,
                  status.tempStatusEffectIds)
            }

            val exp = battle.victoryExperience
            val leveled = game.persistent.givePartyExperience(
              project.data,
              battle.partyIds,
              exp)
            val leveledCharacterNames = leveled.map(getCharacterName)

            val gold = battle.goldDrops
            game.persistent.addRemoveGold(gold)

            val items = battle.generateItemDrops()
            items.foreach(itemId => game.persistent.addRemoveItem(itemId, 1))

            val itemNames = items.map(battle.pData.enums.items(_).name)

            import concurrent.ExecutionContext.Implicits.global

            concurrent.Future {
              scriptInterface.showText(Array("Received %d XP.".format(exp)))
              for (name <- leveledCharacterNames) {
                scriptInterface.showText(Array("%s leveled!".format(name)))
              }

              if (gold > 0)
                scriptInterface.showText(Array("Got %d Gold".format(gold)))

              if (!itemNames.isEmpty) {
                scriptInterface.showText(Array("Got %s.".format(
                    itemNames.mkString(", "))))
              }

              // TODO: endBattle() is called this script. Seems janky.
              scriptInterface.endBattleBackToMap()
            }
          }
        } else {
          battle.advanceTime(delta)

          // Dismiss the current notification if it's done.
          currentNotificationDisplay map { display =>
            if (display.done) {
              currentNotificationDisplay = None
              assert (battle.getNotification.isDefined)
              assert (display.notification == battle.getNotification.get)
              battle.dismissNotification()
            }
          }

          // Add the next notification if it exists.
          if (currentNotificationDisplay.isEmpty) {
            battle.getNotification.map { notification =>
              val source = notification.action.actor

              val windowCounts = collection.mutable.Map[BattleStatus, Int]()
              val windows =
                for (hit <- notification.hits) yield {
                  val box = getBox(hit.hitActor.entityType, hit.hitActor.index)
                  val curWindowCount = windowCounts.getOrElse(hit.hitActor, 0)
                  windowCounts.update(hit.hitActor, curWindowCount + 1)

                  new DamageTextWindow(
                      game.persistent, windowManager,
                      hit.damage.damageString(project.data), box.x, box.y,
                      delayTime = curWindowCount * 0.8f)
                }

              val animations =
                for (hit <- notification.hits; if hit.animationId >= 0) yield {
                  assert(hit.animationId < battle.pData.enums.animations.length)
                  val animation = battle.pData.enums.animations(hit.animationId)

                  val box = getBox(hit.hitActor.entityType, hit.hitActor.index)
                  val player = new AnimationPlayer(project, animation, assets,
                    box.x, box.y)
                  player.play()
                  animationManager.addAnimation(player)
                  player
                }

              notification.action match {
                case action: AttackAction =>
                  val target = notification.action.targets.head

                  postTextNotice("%s attacks %s.".format(
                      getEntityName(source), getEntityName(target)))
                case action: SkillAction =>
                  val skill = battle.pData.enums.skills(action.skillId)
                  postTextNotice("%s uses %s.".format(
                      getEntityName(source), skill.name))
                case action: ItemAction =>
                  val item = battle.pData.enums.items(action.itemId)
                  postTextNotice("%s uses %s.".format(
                      getEntityName(source), item.name))
                case StatusEffectAction =>
                case _ =>
                  postTextNotice("Not implemented yet.")
              }

              val display =
                NotificationDisplay(notification, windows, animations)
              currentNotificationDisplay = Some(display)
            }

          }

          PlayerActionWindow.closeCurrentIfDead()
          PlayerActionWindow.spawnIfNeeded(battle, battle.readyEntity)

          updateEnemyListWindowAndBattlers()
          updatePartyListWindowAndBattlers()
        }
      }
    }
  }

  def render() = {
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL20.GL_BLEND)

    windowManager.render()

    batch.begin()
    animationManager.render(batch)
    batch.end()
  }

  /**
   * Dispose of any disposable resources
   */
  override def dispose() = {
    assertOnBoundThread()

    batch.dispose()

    if (battleActive)
      endBattle()

    super.dispose()
  }
}
