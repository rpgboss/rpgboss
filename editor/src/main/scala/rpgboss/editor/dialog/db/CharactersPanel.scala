package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.editor.dialog._

import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class CharactersPanel(
    owner: Window, 
    sm: StateMaster, 
    initial: ProjectData) 
  extends RightPaneArrayEditingPanel(owner, "Characters", initial.characters)
  with DatabasePanel
{
  def panelName = "Characters"
  def newDefaultInstance() = new Character()
  def label(character: Character) = character.defaultName
  
  def editPaneForItem(idx: Int, initial: Character) = {
    var model = initial
      
    def updateModel(newModel: Character) = {
      model = newModel
      updatePreserveSelection(idx, model)
    }
    
    val leftPane = new DesignGridPanel {
      val fName = new TextField {
        text = model.defaultName
        
        reactions += {
          case ValueChanged(_) =>
            updateModel(model.copy(defaultName = text))
        }
      }
      
      val fSubtitle = new TextField {
        text = model.subtitle
        reactions += {
          case ValueChanged(_) =>
            updateModel(model.copy(subtitle = text))
        }
      }
      
      val fDescription = new TextField {
        text = model.description
        reactions += {
          case ValueChanged(_) =>
            updateModel(model.copy(description = text))
        }
      }
      
      val fSprite = new SpriteBox(
          owner,
          sm,
          model.sprite,
          (newSprite) => {
            updateModel(model.copy(sprite = newSprite))
          })
      
      def numParamEdit(
          initial: Int, 
          mutateF: (Int) => Unit, 
          min: Int = 0, 
          max: Int = 100) = {
        new NumberSpinner(initial, 0, 100, onChange = mutateF)
      }
      
      val fInitLevel = numParamEdit(
          model.initLevel, 
          v => updateModel(model.copy(initLevel = v)),
          MINLEVEL,
          MAXLEVEL)
        
      val fMaxLevel = numParamEdit(
          model.maxLevel, 
          v => updateModel(model.copy(maxLevel = v)),
          MINLEVEL,
          MAXLEVEL)
      
      row().grid(leftLabel("Default name:")).add(fName)
      
      row().grid(leftLabel("Subtitle:")).add(fSubtitle)
      
      row().grid(leftLabel("Description:")).add(fDescription)
      
      row().grid(leftLabel("Sprite:")).add(fSprite)
      
      row()
        .grid(leftLabel("Initial level:")).add(fInitLevel)
        .grid(leftLabel("Max level:")).add(fMaxLevel)
    }
    
    val rightPane = new CharProgressionPanel(model.progressions, p => {
      updateModel(model.copy(progressions = p))
    })
    
    new BoxPanel(Orientation.Horizontal) {
      contents += leftPane
      contents += rightPane
    }
  }
  
  def updated(data: ProjectData) = {
    data.copy(
        characters = array
    )
  }
}