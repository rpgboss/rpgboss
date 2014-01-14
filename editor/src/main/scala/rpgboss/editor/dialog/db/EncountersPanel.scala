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
import rpgboss.player.BattleState
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx

class EncounterFieldGdxPanel(project: Project) extends GdxPanel(640, 320) {
  val battleState = new BattleState(project)
  
  override val gdxListener = new ApplicationAdapter {
    override def render() = {
      battleState.update(Gdx.graphics.getDeltaTime())
      battleState.render()
    }
  }
  
  
}

abstract class EncounterFieldPanel extends Panel {
  def panelWidth: Int
  def panelHeight: Int
  preferredSize = new Dimension(panelWidth, panelHeight)

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
    this.repaint()
  }
    
  override def paintComponent(g: Graphics2D) =
  {
    super.paintComponent(g)
    if (bgImg != null) g.drawImage(bgImg, 0, 0, panelWidth, panelHeight, null)
    
    (units, unitImages).zipped foreach ( (unit, img) => {
      if (img.isDefined) {
        g.drawImage(
          img.get, 
          unit.x - img.get.getWidth() / 2, 
          unit.y - img.get.getHeight() / 2, null)
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
  def battlefieldWidth: Int = 480
  def battlefieldHeight: Int = 320
  
  def panelName = "Encounters"
  def newDefaultInstance() = Encounter()
  def label(e: Encounter) = e.name

  def editPaneForItem(idx: Int, model: Encounter) = {
    def autoArrangeModel(): Unit = {
      if (model.units.isEmpty)
        return
      
      val unitCount = model.units.length
      
      // Divide the battlefield into rectangles, max 2 in y, any number in x.
      val xRects = (unitCount + 1) / 2
      val xRectSize = battlefieldWidth / xRects
      val yRectSize = battlefieldHeight / 2
      
      // Place units onto grid
      val newUnits = for (i <- 0 until unitCount) {
        model.units(i).x = (i / 2) * xRectSize + (xRectSize / 2)
        model.units(i).y = (i % 2) * yRectSize + (yRectSize / 2)
      }
      
      // If there's an odd number, pull the 'last' unit into the center
      if (unitCount % 2 == 1) {
        model.units.last.y = battlefieldHeight / 2
      }
    }
    
    val fDisplay = new EncounterFieldPanel {
      def panelWidth = battlefieldWidth
      def panelHeight = battlefieldHeight
      def update(): Unit = {
        update(model.units, sm.getProj, dbDiag.model)
      }
      update()
    }
    
    val fEnemySelector = new ArrayListView(dbDiag.model.enums.enemies) {
      override def label(a: Enemy) = a.name
    }
    fEnemySelector.selectIndices(0)
    
    val btnAdd = new Button(Action("<= Add") {
      if (!fEnemySelector.selection.indices.isEmpty) {
        val unit = EncounterUnit(fEnemySelector.selection.indices.head, 0, 0)
        model.units = model.units ++ Seq(unit)
        autoArrangeModel()
        fDisplay.update()
      }
    })
    
    val btnRemove = new Button(Action("=> Remove") {
      if (!model.units.isEmpty) {
        model.units = model.units.dropRight(1)
        autoArrangeModel()
        fDisplay.update()
      }
    })
    
    new BoxPanel(Orientation.Horizontal) {
      contents += fDisplay
      contents += new BoxPanel(Orientation.Vertical) {
        contents += btnAdd
        contents += btnRemove
      }
      contents += new BoxPanel(Orientation.Vertical) {
        contents += leftLabel("Enemies:")
        contents += new ScrollPane(fEnemySelector) {
          preferredSize = new Dimension(200, 320)
        }
      }
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.encounters = arrayBuffer
  }
}