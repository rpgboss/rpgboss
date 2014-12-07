package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.dialog.DatabaseDialog
import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.editor.resourceselector._
import rpgboss.editor.Internationalized._

class SystemPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel
  with LazyLogging {
  def panelName = getMessage("Startup")
  layout.labelAlignment(LabelAlignment.RIGHT)

  def model = dbDiag.model

  val fGameTitle = textField(model.title, model.title = _)

  val fTitlepic = new PictureField(owner, sm, model.startup.titlePic,
      model.startup.titlePic = _)

  val fTitleMusic = new MusicField(owner, sm, model.startup.titleMusic,
	  model.startup.titleMusic = _)

  val fScreenW = new NumberSpinner(model.startup.screenW, 640, 1920,
      model.startup.screenW = _)

  val fScreenH = new NumberSpinner(model.startup.screenH, 480, 1080,
      model.startup.screenH = _)

  val fWindowskin = new WindowskinField(owner, sm, model.startup.windowskin,
      model.startup.windowskin = _)

  val fMsgfont = new MsgfontField(owner, sm, model.startup.msgfont,
      model.startup.msgfont = _)

  val fFontsize = new NumberSpinner(model.startup.fontsize, 12, 48,
      model.startup.fontsize = _)

  val fSoundCursor = new SoundField(owner, sm, model.startup.soundCursor,
      model.startup.soundCursor = _)
  val fSoundSelect = new SoundField(owner, sm, model.startup.soundSelect,
	  model.startup.soundSelect = _)
  val fSoundCancel = new SoundField(owner, sm, model.startup.soundCancel,
      model.startup.soundCancel = _)
  val fSoundCannot = new SoundField(owner, sm, model.startup.soundCannot,
      model.startup.soundCannot = _)

  row().grid(lbl(getMessage("Game_Title") + ":")).add(fGameTitle)
  row().grid(lbl(getMessage("Title_Picture") + ":")).add(fTitlepic)
  row().grid(lbl(getMessage("Title_Music") + ":")).add(fTitleMusic)

  row().grid(lbl(getMessage("X_Resolution") + ":")).add(fScreenW)
  row().grid(lbl(getMessage("Y_Resolution") + ":")).add(fScreenH)

  row().grid(lbl(getMessage("Windowskin") + ":")).add(fWindowskin)
  row().grid(lbl(getMessage("Message_Font") + ":")).add(fMsgfont)
  row().grid(lbl(getMessage("Font_Size") + ":")).add(fFontsize)

  row().grid(lbl(getMessage("Cursor_Sound") + ":")).add(fSoundCursor)
  row().grid(lbl(getMessage("Select_Sound") + ":")).add(fSoundSelect)
  row().grid(lbl(getMessage("Cancel_Sound") + ":")).add(fSoundCancel)
  row().grid(lbl(getMessage("Cannot_Sound") + ":")).add(fSoundCannot)
}