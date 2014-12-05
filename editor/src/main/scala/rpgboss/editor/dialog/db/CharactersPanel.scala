package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector.SpriteField

class CharactersPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    dbDiag.model.enums.characters) {
  def panelName = "Characters"
  def newDefaultInstance() = new Character()

  def editPaneForItem(idx: Int, model: Character) = {
    val bioFields = new DesignGridPanel {
      val fName = textField(
        model.name,
        v => {
          model.name = v
          refreshModel()
        })
      val fSubtitle = textField(
        model.subtitle,
        model.subtitle = _)
      val fDescription = textField(
        model.description,
        model.description = _)

      val fSprite = new SpriteField(
        owner,
        sm,
        model.sprite,
        model.sprite = _)

      val fClass = indexedCombo(
        dbDiag.model.enums.classes,
        model.charClass,
        model.charClass = _)

      val fInitLevel = new NumberSpinner(
        model.initLevel,
        MINLEVEL,
        MAXLEVEL,
        model.initLevel = _)

      val fMaxLevel = new NumberSpinner(
        model.maxLevel,
        MINLEVEL,
        MAXLEVEL,
        model.maxLevel = _)

      row().grid(leftLabel("Default name:")).add(fName)

      row().grid(leftLabel("Subtitle:")).add(fSubtitle)

      row().grid(leftLabel("Description:")).add(fDescription)

      row().grid(leftLabel("Sprite:")).add(fSprite)

      row()
        .grid(leftLabel("Class:")).add(fClass)

      row()
        .grid(leftLabel("Initial level:")).add(fInitLevel)
        .grid(leftLabel("Max level:")).add(fMaxLevel)
    }

    val progressionFields = new StatProgressionPanel(model.progressions)

    new BoxPanel(Orientation.Horizontal) with DisposableComponent {
      contents += bioFields
      contents += progressionFields
    }
  }

  override def onListDataUpdate() = {
    logger.info("Characters data updated")
    dbDiag.model.enums.characters = dataAsArray
  }
}