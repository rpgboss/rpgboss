package rpgboss.rpgapplet.lib

import scala.swing._

import net.java.dev.designgridlayout._

class DesignGridPanel extends Panel {
  override lazy val peer = new javax.swing.JPanel with SuperMixin
  val layout = new DesignGridLayout(peer)

  def row() = layout.row()
  
  def addButtons(cancelBtn: Button, okBtn: Button) = 
    row().bar()
      .add(cancelBtn, Tag.CANCEL)
      .add(okBtn, Tag.OK)
  
  implicit def scalaSwingToJava(c: Component) : javax.swing.JComponent = c.peer
  implicit def labelToPeer(l: Label) : javax.swing.JLabel = l.peer
}

