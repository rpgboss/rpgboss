package rpgboss.editor.lib

import scala.swing._
import javax.swing.JPopupMenu
import scala.swing.{ Component, MenuItem }
import scala.swing.SequentialContainer.Wrapper
import javax.swing.event._

object PopupMenu {
  private[PopupMenu] trait JPopupMenuMixin { def popupMenuWrapper: PopupMenu }
}

class PopupMenu extends Component with Wrapper {

  override lazy val peer: JPopupMenu =
    new JPopupMenu with PopupMenu.JPopupMenuMixin with SuperMixin {
      def popupMenuWrapper = PopupMenu.this
    }

  def show(invoker: Component, x: Int, y: Int): Unit =
    peer.show(invoker.peer, x, y)

  def show(invoker: Component, x: Int, y: Int, hideCallback: () => Any) = {
    val listener = new PopupMenuListener {
      def popupMenuWillBecomeVisible(e: PopupMenuEvent) = {
      }
      def popupMenuWillBecomeInvisible(e: PopupMenuEvent) = {
        hideCallback()
        peer.removePopupMenuListener(this)
      }
      def popupMenuCanceled(e: PopupMenuEvent) = {
      }
    }

    peer.addPopupMenuListener(listener)
    peer.show(invoker.peer, x, y)
  }

  /* Create any other peer methods here */
}

