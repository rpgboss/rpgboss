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
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._
import scala.collection.mutable.ArrayBuffer

class AnimationsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Animations",
    dbDiag.model.enums.animations) {
  def panelName = "Animations"
  def newDefaultInstance() = new Animation()

  def editPaneForItem(idx: Int, model: Animation) = {
    val effectsBuffer = ArrayBuffer(model.effects : _*)
    
    val effects = new TableEditor[AnimationEffect]() {
      def title = "Sound and Light Effects"
        
      def modelArray = effectsBuffer
      def newInstance() = AnimationEffect()
      
      def colHeaders = Array("Frame #", "Sound")
      def getRowStrings(row: Int) = {
        val effect = model.effects(row)
        Array(effect.frame.toString, effect.sound.sound)
      }
      def columnCount = 2
      def modelRowCount = model.effects.length
      
    }
    
    new BoxPanel(Orientation.Horizontal) {
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.animations = dataAsArray
  }
}