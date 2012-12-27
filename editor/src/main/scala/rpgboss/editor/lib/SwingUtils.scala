package rpgboss.editor.lib

import scala.swing._
import scala.collection.mutable.Buffer
import rpgboss.model._
import scala.swing.event._

object SwingUtils {
  def lbl(s: String) = new Label(s)
  def leftLabel(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }
  
  def textField(initial: String, onUpdate: String => Unit) = 
    new TextField {
      text = initial
      
      reactions += {
        case ValueChanged(_) => onUpdate(text)
      }
    }
  
  def enumCombo[T <: Enumeration](enum: T)(
      initialId: Int, 
      onUpdate: enum.Value => Any,
      choices: Seq[enum.Value] = Seq()) = {
    
    val actualChoices = if(choices.isEmpty) enum.values.toSeq else choices
    
    new ComboBox(actualChoices) {
      selection.item = enum(initialId)
      reactions += {
        case SelectionChanged(_) => onUpdate(selection.item)
      }
    }
  }
  
  def addBtnsAsGrp(contents: Buffer[Component], btns: List[AbstractButton]) = {
    val firstSelected = btns.find(_.selected)
    val grp = new ButtonGroup(btns : _*)
    
    contents ++= btns
    
    firstSelected.map { btn => grp.select(btn)}
  }
  
  def enumButtons[T <: Enumeration]
      (enum: T)(initial: enum.Value, selectF: enum.Value => Any) = 
  {
    enum.values.toList.map { eVal =>
      new ToggleButton() {
        action = Action(eVal.toString) { 
          selectF(eVal)
        }
        
        selected = eVal == initial
      }
    }
  }
  
  def enumRadios[T <: Enumeration]
      (enum: T)(initial: enum.Value, selectF: enum.Value => Any) = 
  {
    enum.values.toList.map { eVal =>
      new RadioButton() {
        action = Action(eVal.toString) { 
          selectF(eVal)
        }
        
        selected = eVal == initial
      }
    }
  }
  
}