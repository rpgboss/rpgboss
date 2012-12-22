package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class CharactersPanel(
    owner: Window, 
    sm: StateMaster, 
    initial: ProjectData) 
  extends DesignGridPanel 
  with DatabasePanel
{
  def panelName = "Characters"
  layout.labelAlignment(LabelAlignment.RIGHT)
  
  val fGameTitle = new TextField() {
    text = initial.title
  }
  row().grid(lbl("Game title:")).add(fGameTitle)
  
  val fTitlepic = new PictureField(owner, sm, initial.titlePic)
  row().grid(lbl("Title picture:")).add(fTitlepic)
  
  val fWindowskin = new WindowskinField(owner, sm, initial.windowskin)
  row().grid(lbl("Windowskin:")).add(fWindowskin)
  
  val fMsgfont = new MsgfontField(owner, sm, initial.msgfont)
  row().grid(lbl("Message font:")).add(fMsgfont)
  
  val fFontsize = new NumberSpinner(initial.fontsize, 12, 48, 1)
  row().grid(lbl("Font size:")).add(fFontsize)
  
  val fSoundCursor = new SoundField(owner, sm, initial.soundCursor)
  val fSoundSelect = new SoundField(owner, sm, initial.soundSelect)
  val fSoundCancel = new SoundField(owner, sm, initial.soundCancel)
  val fSoundCannot = new SoundField(owner, sm, initial.soundCannot)
  row().grid(lbl("Cursor sound:")).add(fSoundCursor)
  row().grid(lbl("Select sound:")).add(fSoundSelect)
  row().grid(lbl("Cancel sound:")).add(fSoundCancel)
  row().grid(lbl("Cannot sound:")).add(fSoundCannot)
  
  def updated(data: ProjectData) = {
    data.copy(
        title = fGameTitle.text,
        titlePic = fTitlepic.text,
        windowskin = fWindowskin.text,
        msgfont = fMsgfont.text,
        fontsize = fFontsize.getValue,
        soundCursor = fSoundCursor.text,
        soundSelect = fSoundSelect.text,
        soundCancel = fSoundCancel.text,
        soundCannot = fSoundCannot.text
    )
  }
}