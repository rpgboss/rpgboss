package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._
import java.awt.image.BufferedImage
import rpgboss.player.BattleScreen
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import rpgboss.model.battle.Battle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import rpgboss.model.battle.PartyParameters
import rpgboss.editor.Internationalized._
import rpgboss.model.EncounterEvent
import rpgboss.model.EncounterEventMaxFrequency

object EncounterFieldGdxPanel {
  val width = 320
  val height = 160
}

class EncounterFieldGdxPanel(project: Project, initial: Encounter)
  extends GdxPanel(project, EncounterFieldGdxPanel.width,
    EncounterFieldGdxPanel.height) {
  var battleScreen: BattleScreen = null
  var atlasSprites: TextureAtlas = null

  override lazy val gdxListener = new ApplicationAdapter {
    var assets: RpgAssetManager = null

    def updateBattleScreen(encounter: Encounter) = {
      assume(battleScreen != null)

      // TODO: See if this dummy battle constructor has a better home
      val battle = new Battle(
        project.data,
        project.data.startup.startingParty,
        PartyParameters(
          project.data.enums.characters.map(v => 1),
          project.data.enums.characters.map(v => 1),
          project.data.enums.characters.map(v => 1),
          project.data.enums.characters.map(v => Array[Int]()),
          project.data.enums.characters.map(v => Array[Int]()),
          project.data.enums.characters.map(v => Array[Int]()),
          project.data.enums.characters.map(v => 0)),
        encounter,
        aiOpt = None)

      if (battleScreen.battleActive)
        battleScreen.endBattle()

      battleScreen.startBattle(battle, ResourceConstants.defaultBattleback)
    }

    override def create() = {
      assets = new RpgAssetManager(project)
      atlasSprites = GdxUtils.generateSpritesTextureAtlas(
        Spriteset.list(project).map(Spriteset.readFromDisk(project, _)))
      battleScreen = new BattleScreen(
        None,
        assets,
        atlasSprites,
        project,
        EncounterFieldGdxPanel.width,
        EncounterFieldGdxPanel.height) {
        override def drawScale = 0.5f
      }

      // Start visible
      battleScreen.windowManager.transitionAlpha = 0

      updateBattleScreen(initial)
    }

    override def render() = {
      if (assets.update()) {
        battleScreen.update(Gdx.graphics.getDeltaTime())
        battleScreen.render()
      }
    }

    override def dispose() = {
      if (assets != null) {
        assets.dispose()
      }
      super.dispose()
    }
  }

  def updateBattleScreen(encounter: Encounter) = GdxUtils.asyncRun {
    gdxListener.updateBattleScreen(encounter)
  }
}

class EncountersPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.encounters) {
  def battlefieldWidth: Int = 350
  def battlefieldHeight: Int = 220
  def yOffset = 100
  def xOffset = 80

  def panelName = getMessage("Encounters")
  def newDefaultInstance() = Encounter()

  def editPaneForItem(idx: Int, model: Encounter) = {
    def autoArrangeModel(): Unit = {
      if (model.units.isEmpty)
        return

      val unitCount = model.units.length

      // Divide the battlefield into rectangles, max 2 in y, any number in x.
      val xRects = (unitCount + 1) / 2
      val xRectSize = battlefieldWidth / xRects
      val yRectSize = battlefieldHeight / 2

      val xOffsetPerRow = -15

      // Place units onto grid
      val newUnits = for (i <- 0 until unitCount) {
        val col = i / 2
        val row = i % 2
        model.units(i).x =
          (col * xRectSize) + (xRectSize / 2) + (xOffsetPerRow * row) + xOffset
        model.units(i).y = (i % 2) * yRectSize + (yRectSize / 2) + yOffset
      }

      // If there's an odd number, pull the 'last' unit into the center
      if (unitCount % 2 == 1) {
        model.units.last.y = yRectSize + yOffset
      }
    }

    val fName = textField(model.name, model.name = _, Some(refreshModel))

    val fEscapeChance =
      percentField(0.01f, 1, model.escapeChance, model.escapeChance = _)

    def updateFields() = {
      fEscapeChance.enabled = model.canEscape
    }
    updateFields()

    val fCanEscape = boolField(
      getMessage("Can_Escape"), model.canEscape, model.canEscape = _,
      Some(updateFields))

    def regenerateName(): Unit = {
      if (!model.name.isEmpty && !model.name.startsWith("#"))
        return

      if (model.units.isEmpty)
        fName.text = ""

      val enemyLabels = Encounter.getEnemyLabels(model.units, dbDiag.model)

      fName.text = "# %s".format(enemyLabels.mkString(", "))
    }

    val fDisplay = new EncounterFieldGdxPanel(sm.getProj, model)

    val fEnemySelector = new ArrayListView(dbDiag.model.enums.enemies) {
      override def label(a: Enemy) = a.name
    }
    fEnemySelector.selectIndices(0)

    val btnAdd = new Button(Action(getMessage("Add")) {
      if (!fEnemySelector.selection.indices.isEmpty) {
        val unit = EncounterUnit(fEnemySelector.selection.indices.head, 0, 0)
        model.units = model.units ++ Seq(unit)
        autoArrangeModel()
        regenerateName()
        fDisplay.updateBattleScreen(model)
      }
    })

    val btnRemove = new Button(Action(getMessage("Remove")) {
      if (!model.units.isEmpty) {
        model.units = model.units.dropRight(1)
        autoArrangeModel()
        regenerateName()
        fDisplay.updateBattleScreen(model)
      }
    })

    new BoxPanel(Orientation.Horizontal) with DisposableComponent {

      contents += new BoxPanel(Orientation.Vertical) {
        contents += new DesignGridPanel {
          row().grid(lbl(getMessageColon("Encounter_Name"))).add(fName)
          row().grid().add(fCanEscape)
          row()
            .grid(lbl(getMessageColon("Escape_Chance")))
            .add(fEscapeChance)
        }

        contents +=
          new EncounterEventArrayPanel(dbDiag, model.events, model.events = _)
      }
      contents += new BoxPanel(Orientation.Vertical) {
        contents += fDisplay

        contents += new BoxPanel(Orientation.Horizontal) {
          contents += btnAdd
          contents += btnRemove
        }

        contents += new ScrollPane(fEnemySelector) {
          preferredSize = new Dimension(200, 120)
        }
      }

      override def dispose() = {
        fDisplay.dispose()
        super.dispose()
      }
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.encounters = dataAsArray
  }
}

class EncounterEventArrayPanel(
    dbDiag: DatabaseDialog,
    initial: Array[EncounterEvent],
    onUpdate: Array[EncounterEvent] => Unit)
  extends InlineWidgetArrayEditor(dbDiag, initial, onUpdate) {
  override def title = getMessage("Events")
  override def addAction(index: Int) = insertElement(index, EncounterEvent())
  override def newInlineWidget(model: EncounterEvent) =
    new EncounterEventPanel(dbDiag, model, sendUpdate)
}

/**
 * Updates model in-place.
 */
class EncounterEventPanel(
  dbDiag: DatabaseDialog,
  initial: EncounterEvent,
  onUpdate: () => Unit)
  extends BoxPanel(Orientation.Vertical) {
  val model = initial

  contents += lbl("Encounter Event")
  contents += new Button(Action(getMessage("Edit")) {
    val d = new EncounterEventDialog(dbDiag, dbDiag.stateMaster, model)
    d.open()
  })
}

class EncounterEventDialog(
  owner: Window,
  sm: StateMaster,
  model: EncounterEvent)
  extends StdDialog(owner, getMessageColon("Edit_Encounter_Event")) {

  centerDialog(new Dimension(600, 600))

  def okFunc() = {
    close()
  }

  val fMaxFrequency = enumIdCombo(EncounterEventMaxFrequency)(
    model.maxFrequency, model.maxFrequency = _)

  val commandBox = new CommandBox(
    owner,
    sm,
    None,
    model.cmds,
    model.cmds = _,
    inner = false)

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new BoxPanel(Orientation.Vertical) {
        contents += lbl(getMessageColon("Max_Frequency"))
        contents += fMaxFrequency

        contents += new ConditionsPanel(
          owner, sm.getProjData, model.conditions, model.conditions = _)
      }

      contents += new DesignGridPanel {
        row.grid.add(leftLabel(getMessageColon("Commands")))
        row.grid.add(new ScrollPane {
          preferredSize = new Dimension(400, 400)
          contents = commandBox
        })
      }
    }

    contents += new DesignGridPanel {
      row().bar().add(okBtn, Tag.OK)
    }
  }
}