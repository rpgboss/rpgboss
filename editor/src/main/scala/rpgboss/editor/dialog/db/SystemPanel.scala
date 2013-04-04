package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog
import com.typesafe.scalalogging.slf4j.Logging

class SystemPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends DesignGridPanel 
  with DatabasePanel
  with Logging
{
  def panelName = "Startup"
  layout.labelAlignment(LabelAlignment.RIGHT)
  
  def model = dbDiag.model
  
  def updateModel(m: ProjectData) = {
    dbDiag.model = m
  }
  
  val fGameTitle = new TextField() {
    text = model.title
    reactions += {
      case EditDone(_) => updateModel(model.copy(title = text))
    }
  }
  
  val fTitlepic = new PictureField(owner, sm, model.titlePic, v => {
    updateModel(model.copy(titlePic = v))
  })
  val fWindowskin = new WindowskinField(owner, sm, model.windowskin, v => {
    updateModel(model.copy(windowskin = v))
  })
  
  val fMsgfont = new MsgfontField(owner, sm, model.msgfont, v => {
    updateModel(model.copy(msgfont = v))
  })
  
  val fFontsize = new NumberSpinner(model.fontsize, 12, 48, onUpdate = { v =>
    updateModel(model.copy(fontsize = v))
  })
  
  val fSoundCursor = new SoundField(owner, sm, model.soundCursor, v => {
    updateModel(model.copy(soundCursor = v))
  })
  val fSoundSelect = new SoundField(owner, sm, model.soundSelect, v => {
    updateModel(model.copy(soundSelect = v))
  })
  val fSoundCancel = new SoundField(owner, sm, model.soundCancel, v => {
    updateModel(model.copy(soundCancel = v))
  })
  val fSoundCannot = new SoundField(owner, sm, model.soundCannot, v => {
    updateModel(model.copy(soundCannot = v))
  })
  
  row().grid(lbl("Game title:")).add(fGameTitle)
  row().grid(lbl("Title picture:")).add(fTitlepic)
  row().grid(lbl("Windowskin:")).add(fWindowskin)
  row().grid(lbl("Message font:")).add(fMsgfont)
  row().grid(lbl("Font size:")).add(fFontsize)
  
  row().grid(lbl("Cursor sound:")).add(fSoundCursor)
  row().grid(lbl("Select sound:")).add(fSoundSelect)
  row().grid(lbl("Cancel sound:")).add(fSoundCancel)
  row().grid(lbl("Cannot sound:")).add(fSoundCannot)
}