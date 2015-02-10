package rpgboss.editor.util

object MouseUtil {
  def isRightClick(e: scala.swing.event.MouseEvent): Boolean = {
    return e.peer.getButton() == java.awt.event.MouseEvent.BUTTON3 ||
        e.peer.isControlDown()
  }
}