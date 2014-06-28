package rpgboss.editor.dialog

import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.event._
import rpgboss.editor.dialog.cmd._
import rpgboss.editor.StateMaster
import rpgboss.editor.dialog.cmd.NewEvtCmdBox
import java.awt.event.MouseEvent
import rpgboss.editor.uibase.RpgPopupMenu

/**
 * Holds and edits list of commands.
 * Current state is held in listData member.
 */
class CommandBox(
  evtDiag: EventDialog,
  owner: Window,
  sm: StateMaster,
  mapName: String,
  initialCmds: Seq[EventCmd])
  extends ListView(initialCmds) {

  listenTo(mouse.clicks)

  def newCmdDialog() = {
    val d = new NewEvtCmdBox(evtDiag, sm, owner, mapName, this,
                             selection.indices.head)
    d.open()
  }

  def editSelectedCmd() = {
    val selectedIdx = selection.indices.head
    val selectedCmd = selection.items.head
    val d = EventCmdDialog.dialogFor(owner, sm, mapName, selectedCmd,
      newEvt => {
        listData = listData.updated(selectedIdx, newEvt)
      })
    d.open()
  }

  val cmdBox = this
  reactions += {
    case e: MouseClicked =>
      if (e.peer.getButton() == MouseEvent.BUTTON3) {
        // List won't automatically select
        val closestI = this.peer.locationToIndex(e.point)
        if (closestI != -1) {
          this.selectIndices(closestI)

          val menu = new RpgPopupMenu {
            contents += new MenuItem(Action("Delete") {
              if (!selection.items.isEmpty &&
                  selection.items.head != EndOfScript()) {
                listData = rpgboss.lib.Utils.removeFromSeq(
                listData, selection.indices.head)
              }
            })
          }

          menu.show(this, e.point.x, e.point.y)
        }

      } else if (e.clicks == 2) {
        selection.items.head match {
          case c: EndOfScript => newCmdDialog()
          case _ => editSelectedCmd()
        }
      }
  }

  def insertCmd(idx: Int, cmd: EventCmd) = {
    val newList =
      listData.take(idx) ++ Seq(cmd) ++ listData.takeRight(listData.length - idx)

    listData = newList
  }
}