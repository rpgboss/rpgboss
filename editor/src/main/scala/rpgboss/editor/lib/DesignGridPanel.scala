package rpgboss.editor.lib

import scala.swing._

import net.java.dev.designgridlayout._

class DesignGridPanel extends Panel {
  override lazy val peer = new javax.swing.JPanel with SuperMixin
  val layout = new DesignGridLayout(peer)

  def row() = layout.row()
  
  def addButtons(
      cancelBtn: Button, 
      okBtn: Button, 
      applyBtn: Option[Button] = None) = {
    val rowBar = row().bar()
    rowBar.add(cancelBtn, Tag.CANCEL).add(okBtn, Tag.OK)
    applyBtn.map { b => rowBar.add(b, Tag.APPLY) }
  }
  
  def addCancel(cancelBtn: Button) = 
    row().bar()
      .add(cancelBtn, Tag.CANCEL)
  
  implicit def scalaSwingToJava(c: Component) : javax.swing.JComponent = c.peer
  implicit def labelToPeer(l: Label) : javax.swing.JLabel = l.peer
}

