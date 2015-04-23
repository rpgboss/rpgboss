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
  def panelName = getMessage("System")
  layout.labelAlignment(LabelAlignment.RIGHT)

  def model = dbDiag.model

  val fGameTitle = textField(model.title, model.title = _)

  val fTitlepic = new PictureField(owner, sm, model.startup.titlePic,
      model.startup.titlePic = _)

  val fTitleMusic = new MusicField(owner, sm, model.startup.titleMusic,
	  model.startup.titleMusic = _)

  val fGameOverpic = new PictureField(owner, sm, model.startup.gameOverPic,
      model.startup.gameOverPic = _)

  val fGameOverMusic = new MusicField(owner, sm, model.startup.gameOverMusic,
    model.startup.gameOverMusic = _)

  val fScreenW = new NumberSpinner(640, 1920, model.startup.screenW,
      model.startup.screenW = _)

  val fScreenH = new NumberSpinner(480, 1080, model.startup.screenH,
      model.startup.screenH = _)

  val fWindowskin = new WindowskinField(owner, sm, model.startup.windowskin,
      model.startup.windowskin = _)

  val fMsgfont = new MsgfontField(owner, sm, model.startup.msgfont,
      model.startup.msgfont = _)

  val fFontsize = new NumberSpinner(12, 48, model.startup.fontsize,
      model.startup.fontsize = _)

  val fStringInputCharacters = textField(
      model.startup.stringInputCharacters,
      model.startup.stringInputCharacters = _)

  val fSoundCursor = new SoundField(owner, sm, model.startup.soundCursor,
      model.startup.soundCursor = _)
  val fSoundSelect = new SoundField(owner, sm, model.startup.soundSelect,
	  model.startup.soundSelect = _)
  val fSoundCancel = new SoundField(owner, sm, model.startup.soundCancel,
      model.startup.soundCancel = _)
  val fSoundCannot = new SoundField(owner, sm, model.startup.soundCannot,
      model.startup.soundCannot = _)

  row().grid(lbl(getMessageColon("Game_Title"))).add(fGameTitle)
  row().grid(lbl(getMessageColon("Title_Picture"))).add(fTitlepic)
  row().grid(lbl(getMessageColon("Title_Music"))).add(fTitleMusic)
  row().grid(lbl(getMessageColon("GameOver_Picture"))).add(fGameOverpic)
  row().grid(lbl(getMessageColon("GameOver_Music"))).add(fGameOverMusic)

  row().grid(lbl(getMessageColon("X_Resolution"))).add(fScreenW)
  row().grid(lbl(getMessageColon("Y_Resolution"))).add(fScreenH)

  row().grid(lbl(getMessageColon("Windowskin"))).add(fWindowskin)
  row().grid(lbl(getMessageColon("Message_Font"))).add(fMsgfont)
  row().grid(lbl(getMessageColon("Font_Size"))).add(fFontsize)

  row()
    .grid(lbl(getMessageColon("String_Input_Characters")))
    .add(fStringInputCharacters )

  row().grid(lbl(getMessageColon("Cursor_Sound"))).add(fSoundCursor)
  row().grid(lbl(getMessageColon("Select_Sound"))).add(fSoundSelect)
  row().grid(lbl(getMessageColon("Cancel_Sound"))).add(fSoundCancel)
  row().grid(lbl(getMessageColon("Cannot_Sound"))).add(fSoundCannot)
}