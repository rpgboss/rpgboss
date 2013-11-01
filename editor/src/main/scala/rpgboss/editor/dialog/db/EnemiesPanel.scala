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

class EnemiesPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Enemies",
    dbDiag.model.enums.enemies) {
  def panelName = "Enemies"
  def newDefaultInstance() = new Enemy()
  def label(e: Enemy) = e.name

  def editPaneForItem(idx: Int, model: Enemy) = {
    val bioFields = new DesignGridPanel {
      val fName = textField(
        model.name,
        v => {
          model.name = v
          refreshModel()
        })

      val fBattler = new BattlerField(
        owner,
        sm,
        model.battler,
        model.battler = _)

      val fLevel = new NumberSpinner(model.level, 1, 50, model.level = _)
      val fMhp = new NumberSpinner(model.mhp, 5, 5000, model.mhp = _)
      val fMmp = new NumberSpinner(model.mmp, 0, 500, model.mhp = _)
      val fStr = new NumberSpinner(model.str, 1, 50, model.str = _)
      val fDex = new NumberSpinner(model.dex, 1, 50, model.dex = _)
      val fCon = new NumberSpinner(model.con, 1, 50, model.con = _)
      val fInt = new NumberSpinner(model.int, 1, 50, model.int = _)
      val fWis = new NumberSpinner(model.wis, 1, 50, model.wis = _)
      val fCha = new NumberSpinner(model.cha, 1, 50, model.cha = _)
      
      val fExpValue = 
        new NumberSpinner(model.expValue, 10, 50000, model.expValue = _)
      
      row().grid(leftLabel("Name:")).add(fName)
      
      row().grid(leftLabel("Battler:")).add(fBattler)
      
      row().grid(leftLabel("Level:")).add(fLevel)

      row().grid(leftLabel("Max HP:")).add(fMhp)
      row().grid(leftLabel("Max MP:")).add(fMmp)

      row().grid(leftLabel("STR:")).add(fStr)
      row().grid(leftLabel("DEX:")).add(fDex)
      row().grid(leftLabel("CON:")).add(fCon)
      row().grid(leftLabel("INT:")).add(fInt)
      row().grid(leftLabel("WIS:")).add(fWis)
      row().grid(leftLabel("CHA:")).add(fCha)
      
      row().grid(leftLabel("EXP Value:")).add(fExpValue)
    }
    
    val fEffects =
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _)

    new BoxPanel(Orientation.Horizontal) {
      contents += bioFields
      contents += fEffects
    }
  }

  override def onListDataUpdate() = {
    logger.info("Enemies data updated")
    dbDiag.model.enums.enemies = arrayBuffer
  }
}