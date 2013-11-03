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
import rpgboss.editor.resourceselector._
import java.awt.image.BufferedImage

class EncounterFieldPanel extends Panel {
  preferredSize = new Dimension(480, 320)

  val bgImg = rpgboss.lib.Utils.readClasspathImage(
    "defaultrc/picture/defaultrc_battleback/crownlesswish_rrr.jpg")

  var units: Seq[EncounterUnit] = Seq()
  var unitImages: Seq[Option[BufferedImage]] = Seq()
    
  def update(units: Seq[EncounterUnit], proj: Project, pData: ProjectData) = {
    this.units = units
    unitImages = units.map(unit => {
      if (unit.enemyIdx < pData.enums.enemies.length) {
        pData.enums.enemies(unit.enemyIdx).battler.map { battler => 
          Battler.readFromDisk(proj, battler.name).img
        }
      } else None
    })
  }
    
  override def paintComponent(g: Graphics2D) =
  {
    super.paintComponent(g)
    if (bgImg != null) g.drawImage(bgImg, 0, 0, null)
    
    (units, unitImages).zipped foreach ( (unit, img) => {
      if (img.isDefined) {
        g.drawImage(img.get, unit.x, unit.y, null)
      }
    })
  }
}

class EncountersPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Encounters",
    dbDiag.model.enums.encounters) {
  def panelName = "Encounters"
  def newDefaultInstance() = Encounter()
  def label(e: Encounter) = e.name

  def editPaneForItem(idx: Int, model: Encounter) = {
    val fDisplay = new EncounterFieldPanel
    
    val fEnemySelector = new ArrayListView(dbDiag.model.enums.enemies) {
      override def label(a: Enemy) = a.name
    }
    
    new BoxPanel(Orientation.Vertical) {
      contents += fDisplay
      contents += fEnemySelector
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.encounters = arrayBuffer
  }
}