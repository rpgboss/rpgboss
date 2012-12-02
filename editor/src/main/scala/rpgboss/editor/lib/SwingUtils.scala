package rpgboss.editor.lib

import scala.swing._
import scala.collection.mutable.Buffer
import rpgboss.model._

object SwingUtils {
  def leftLabel(s: String) = new Label(s) {
    xAlignment = Alignment.Left
  }
  
  def addBtnsAsGrp(contents: Buffer[Component], btns: List[AbstractButton]) = {
    val grp = new ButtonGroup(btns : _*)
    grp.select(btns.head)
    
    contents ++= btns
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