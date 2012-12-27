package rpgboss.editor.lib

import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import com.weiglewilczek.slf4s.Logging
import java.awt.{Font, Color}
import rpgboss.model._

class CharProgressionPanel(
    initial: CharProgressions, 
    onUpdate: (CharProgressions) => Unit)
  extends DesignGridPanel 
{
  //border = BorderFactory.createTitledBorder("Stat progressions")
  
  var model = initial
  def updateModel(newModel: CharProgressions) = {
    model = newModel
    onUpdate(model)
  }
  
  def progressionEditor(
      label: String, 
      initial: Curve, 
      onUpdate: (Curve) => Unit) = 
  {
    var model = initial
    
    val lvl1Val = new TextField {
      editable = false
      text = model(1).toString
    }
    
    val lvl50Val = new TextField {
      editable = false
      text = model(50).toString
    }
    
    def numSpinner(initial: Int, mutateF: (Int) => Unit) = {
      new NumberSpinner(initial, 0, 100, onUpdate = { v =>
        mutateF(v)
        onUpdate(model)
        
        lvl1Val.text = model(1).toString
        lvl50Val.text = model(50).toString
      })
    }
    
    new DesignGridPanel {
      border = BorderFactory.createTitledBorder(label)
      
      val aSpin = numSpinner(initial.a, v => model = model.copy(a = v))
      val bSpin = numSpinner(initial.b, v => model = model.copy(b = v))
      val cSpin = numSpinner(initial.c, v => model = model.copy(c = v))
      
      /*row()
        .grid().add(leftLabel("a:")).add(leftLabel("b:")).add(leftLabel("c:"))
      row()
        .grid().add(aSpin).add(bSpin).add(cSpin)*/
      
      row().grid(leftLabel("a =")).add(aSpin)
      row().grid(leftLabel("b =")).add(bSpin)
      row().grid(leftLabel("c =")).add(cSpin)
      row().grid(leftLabel("At L1:")).add(lvl1Val)
      row().grid(leftLabel("At L50:")).add(lvl50Val)
    }
  }
  
  val fExp = progressionEditor(
      "Exp", model.exp, p => updateModel(model.copy(exp = p)))
  val fHp = progressionEditor(
      "HP",  model.hp,  p => updateModel(model.copy(hp  = p)))
  val fSp = progressionEditor(
      "SP",  model.sp,  p => updateModel(model.copy(sp  = p)))
  val fStr = progressionEditor(
      "Str", model.str, p => updateModel(model.copy(str = p)))
  val fDex = progressionEditor(
      "Dex", model.dex, p => updateModel(model.copy(dex = p)))
  val fCon = progressionEditor(
      "Con", model.con, p => updateModel(model.copy(con = p)))
  val fInt = progressionEditor(
      "Int", model.int, p => updateModel(model.copy(int = p)))
  val fWis = progressionEditor(
      "Wis", model.wis, p => updateModel(model.copy(wis = p)))
  val fCha = progressionEditor(
      "Cha", model.cha, p => updateModel(model.copy(cha = p)))
  
  row().grid().add(fExp, fHp,  fSp)
  row().grid().add(fStr, fDex, fCon)
  row().grid().add(fInt, fWis, fCha)
  row().grid().add(new Label("y = a*x^2 + b*x + c"), 3)
}