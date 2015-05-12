package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.Internationalized._
import rpgboss.editor.resourceselector.SpriteField
import javax.swing.BorderFactory

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
        BorderFactory.createTitledBorder(getMessage("Vehicle %d".format(i)))

      val model = dbDiag.model.vehicles(i)
      val fSprite = new SpriteField(owner, sm, model.sprite, model.sprite = _)
      val fName = textField(model.name, model.name = _)
      val fCanFly =
        boolField(getMessage("Can Fly"), model.canFly, model.canFly = _)
      row().grid(lbl(getMessageColon("Sprite"))).add(fSprite)
      row().grid(lbl(getMessageColon("Name"))).add(fName)
      row().grid().add(fCanFly)
    }

    contents += fPanel
  }
}