package rpgboss.editor.dialog.db

import scala.swing.GridPanel
import scala.swing.Window

import javax.swing.BorderFactory
import rpgboss.editor.Internationalized.getMessage
import rpgboss.editor.Internationalized.getMessageColon
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.resourceselector.SpriteField
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.SwingUtils.boolField
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.editor.uibase.SwingUtils.textField
import rpgboss.model.Constants

class VehiclesPane(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends GridPanel(2, 2)
  with DatabasePanel {
  def panelName = getMessage("Vehicles")

  for (i <- 0 until Constants.NUM_VEHICLES) {
    val fPanel = new DesignGridPanel {
      border =
        BorderFactory.createTitledBorder(getMessage("Vehicle_%d".format(i)))

      val model = dbDiag.model.vehicles(i)
      val fSprite = new SpriteField(owner, sm, model.sprite, model.sprite = _)
      val fName = textField(model.name, model.name = _)
      val fCanFly =
        boolField(getMessage("Can_Fly"), model.canFly, model.canFly = _)
      row().grid(lbl(getMessageColon("Sprite"))).add(fSprite)
      row().grid(lbl(getMessageColon("Name"))).add(fName)
      row().grid().add(fCanFly)
    }

    contents += fPanel
  }
}