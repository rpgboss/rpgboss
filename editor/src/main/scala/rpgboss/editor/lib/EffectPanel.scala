package rpgboss.editor.lib

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import com.weiglewilczek.slf4s.Logging
import java.awt.{Font, Color}
import javax.swing.table.AbstractTableModel
import rpgboss.model.Effect
import rpgboss.model.RpgEnum
import rpgboss.model.Constants._

class EffectPanel(
    owner: Window, 
    dbDiag: DatabaseDialog,
    initial: Array[Effect],
    onUpdate: Array[Effect] => Unit) 
  extends BoxPanel(Orientation.Vertical)
{
  border = BorderFactory.createTitledBorder("Effects")
  
  var effects = initial
  val table = new Table() {
    val tableModel = new AbstractTableModel() {
      val colNames = Array("Description", "Key", "Value")
      
      def getRowCount() = effects.size + 1 // last element blank for adding
      def getColumnCount() = 3
      override def getColumnName(col: Int) = colNames(col)
      
      def getValueAt(row: Int, col: Int) = {
        if(row < effects.size) {
          val eff = effects(row)
          col match {
            case 0 => EffectKey.withName(eff.key).desc
            case 1 => eff.key
            case 2 => eff.v.toString
          }
        } else {
          "" // blank for new row
        }
      }
      
      override def isCellEditable(r: Int, c: Int) = false
    }
    
    model = tableModel 
    
    selection.elementMode = Table.ElementMode.Row
    selection.intervalMode = Table.IntervalMode.Single
    
    listenTo(mouse.clicks)
    reactions += {
      case MouseClicked(_, _, _, 2, _) => {
        val row = selection.rows.head
        if(row < effects.size) {
          val initialE = effects(row)
          
          val diag = new EffectDialog(
              owner,
              dbDiag,
              initialE,
              e => {
                effects.update(row, e)
                onUpdate(effects)
                tableModel.fireTableRowsUpdated(row, row)
              }
          )
          diag.open()
          
        } else {
          val diag = new EffectDialog(
              owner,
              dbDiag,
              EffectKey.defaultEffect,
              e => {
                effects = effects ++ Array(e) 
                onUpdate(effects)
                tableModel.fireTableRowsUpdated(row, row)
                tableModel.fireTableRowsInserted(row+1, row+1)
              }
          )
          diag.open()
        }
      }
    }
  }
  
  contents += new ScrollPane {
    contents = table
  }
}

class EffectDialog(
    owner: Window, 
    dbDiag: DatabaseDialog, 
    initial: Effect, 
    onOk: Effect => Unit)
  extends StdDialog(owner, "Edit Effect")
{
  case class EffectControls(
      key: EffectKey.Val, 
      btn: AbstractButton, 
      control: Component,
      getVal: () => Int,
      setVal: (Int) => Unit)
  
  var model = initial
  var controls = Nil
  var selectedControls: EffectControls = null
  
  def selectKey(key: EffectKey.Val) = {
    selectedControls = effectsMap.get(key.toString).get
    
    btnGroup.select(selectedControls.btn)
    
    effectsAll.foreach { eControls =>
      eControls.control.enabled = false
    }
    
    selectedControls.control.enabled = true
    
    model = model.copy(key = key.toString, v = selectedControls.getVal())
  }
  
  private def newRadioForKey(key: EffectKey.Val) = new RadioButton() {
    action = Action(key.desc) { 
      selectKey(key)
    }
  }
  
  def onCurControlChange() = {
    model = model.copy(v = selectedControls.getVal())
  }
  
  def nilEffect(key: EffectKey.Val): EffectControls = {
    EffectControls(
        key, 
        newRadioForKey(key), 
        new BoxPanel(Orientation.Vertical),
        () => 0,
        v => Unit)
  }
  
  def intEffect(key: EffectKey.Val): EffectControls = {
    val spinner = new NumberSpinner(
        0, 
        MINEFFECTARG, 
        MAXEFFECTARG,
        onUpdate = v => onCurControlChange())
    
    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("p") {
        preferredSize = new Dimension(15, 15)
      }
      
      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }
    
    EffectControls(
        key, 
        newRadioForKey(key), 
        control,
        () => spinner.getValue,
        v => spinner.setValue(v))
  }
  
  def percentEffect(key: EffectKey.Val): EffectControls = {
    val spinner = new NumberSpinner(
        0, 
        -100, 
        100,
        onUpdate = v => onCurControlChange())
    
    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("%") {
        preferredSize = new Dimension(15, 15)
      }
      
      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }
    
    EffectControls(
        key, 
        newRadioForKey(key), 
        control,
        () => spinner.getValue,
        v => spinner.setValue(v))
  }
  
  def choiceEffect[T](key: EffectKey.Val, choices: Seq[T]): EffectControls = {
    val combo = indexedCombo(choices, 0, i => onCurControlChange())
    
    EffectControls(
        key, 
        newRadioForKey(key), 
        combo,
        () => combo.selection.index,
        combo.selection.index = _)
  }
  
  import EffectKey._
  
  val effectsStatus = Array(
      intEffect(RecoverHpAdd),
      percentEffect(RecoverHpMul),
      intEffect(RecoverMpAdd),
      percentEffect(RecoverMpMul),
      choiceEffect(AddStatusEffect, dbDiag.model.statusEffects),
      choiceEffect(RemoveStatusEffect, dbDiag.model.statusEffects)
  )
  
  val effectsStats = Array(
      intEffect(MhpAdd),
      intEffect(MmpAdd),
      intEffect(StrAdd),
      intEffect(DexAdd),
      intEffect(ConAdd),
      intEffect(IntAdd),
      intEffect(WisAdd),
      intEffect(ChaAdd)
  )
  
  val effectsOther = Array(
      nilEffect(EscapeBattle),
      choiceEffect(UseSkill, dbDiag.model.skills),
      choiceEffect(LearnSkill, dbDiag.model.skills)
  )
  
  val effectsAll = effectsStatus ++ effectsStats ++ effectsOther
  val effectsMap = Map(effectsAll.map(x => x.key.toString->x) : _*)
  
  val btnGroup = new ButtonGroup(effectsAll.map(_.btn) : _*)
  
  // Does initialization of dialog
  {
    effectsMap.get(initial.key) map { ctrlGrp =>
      selectKey(ctrlGrp.key)
      ctrlGrp.setVal(initial.v)
    }
  }
  
  class ControlPage(label: String, val controls: Seq[EffectControls])
  {
    val panel = new DesignGridPanel {
      controls.foreach { eControls =>
        row()
          .grid().add(eControls.btn)
          .grid().add(eControls.control)
      }
    }
    val tabPage = new TabbedPane.Page(label, panel)
  }
  
  val ctlPages = Array(
      new ControlPage("Status", effectsStatus),
      new ControlPage("Stats",  effectsStats),
      new ControlPage("Other",  effectsOther)
  )
  
  contents = new DesignGridPanel {
    val tabPane = new TabbedPane {
      pages ++= ctlPages.map(_.tabPage)
    } 
    
    row().grid().add(tabPane)
    
    addButtons(cancelBtn, okBtn)
  }
  
  def okFunc() = {
    onOk(model)
    close()
  }
}

object EffectKey extends RpgEnum {
  case class Val(desc: String) extends super.Val
  
  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]
  
  val RecoverHpAdd = Val("Recover HP")
  val RecoverHpMul = Val("Recover percentage of HP")
  val RecoverMpAdd = Val("Recover MP")
  val RecoverMpMul = Val("Recover percentage of MP")
  
  val AddStatusEffect = Val("Add status effect")
  val RemoveStatusEffect = Val("Remove status effect")
  
  val MhpAdd = Val("Increase Max HP")
  val MmpAdd = Val("Increase Max MP")
  val StrAdd = Val("Increase STR")
  val DexAdd = Val("Increase DEX")
  val ConAdd = Val("Increase CON")
  val IntAdd = Val("Increase INT")
  val WisAdd = Val("Increase WIS")
  val ChaAdd = Val("Increase CHA")
  
  val EscapeBattle = Val("Escape battle")
  
  val UseSkill = Val("Use skill")
  val LearnSkill = Val("Learn skill") 
  
  def default = RecoverHpAdd
  
  def defaultEffect = Effect(RecoverHpAdd.toString, 0)
}