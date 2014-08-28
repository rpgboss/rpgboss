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

class SystemPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends DesignGridPanel
  with DatabasePanel
  with LazyLogging {
  def panelName = "Startup"
  layout.labelAlignment(LabelAlignment.RIGHT)

  def model = dbDiag.model

  val fGameTitle = new TextField() {
    text = model.title
    reactions += {
      case EditDone(_) => model.title = text
    }
  }

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

  row().grid(lbl("Game title:")).add(fGameTitle)
  row().grid(lbl("Title picture:")).add(fTitlepic)
  row().grid(lbl("Title music:")).add(fTitleMusic)

  row().grid(lbl("X Resolution:")).add(fScreenW)
  row().grid(lbl("Y Resolution:")).add(fScreenH)

  row().grid(lbl("Windowskin:")).add(fWindowskin)
  row().grid(lbl("Message font:")).add(fMsgfont)
  row().grid(lbl("Font size:")).add(fFontsize)

  row().grid(lbl("Cursor sound:")).add(fSoundCursor)
  row().grid(lbl("Select sound:")).add(fSoundSelect)
  row().grid(lbl("Cancel sound:")).add(fSoundCancel)
  row().grid(lbl("Cannot sound:")).add(fSoundCannot)
}