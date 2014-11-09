package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.uibase._

import org.fife.ui.rtextarea._;
import org.fife.ui.rsyntaxtextarea._;

import java.awt.Toolkit

class RunJsCmdDialog(
  owner: Window,
  initial: RunJs,
  successF: (RunJs) => Any)
  extends StdDialog(owner, "Run Javascript") {

  val screenSize = Toolkit.getDefaultToolkit().getScreenSize();

  centerDialog(new Dimension(screenSize.width/2, screenSize.height/2))

  val textArea = new RSyntaxTextArea(20, 60);
  textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
  textArea.setCodeFoldingEnabled(true);
  val sp = new RTextScrollPane(textArea);

  def okFunc() = {
    successF(RunJs(textArea.getText()))
    close()
  }

  contents = new DesignGridPanel {
    //row().grid().add(leftLabel("Script:"))
    //row().grid().add(textEditScroll)
    row().grid().add(sp);

    addButtons(cancelBtn, okBtn)
  }

}