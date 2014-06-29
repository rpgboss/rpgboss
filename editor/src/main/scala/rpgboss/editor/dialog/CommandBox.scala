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
  initialCmds: Array[EventCmd])
  extends GridPanel(1, 1) {

  val listView = new ListView[EventCmd] {
    listData = initialCmds ++ List(EndOfScript())
  }

  def getEventCmds: Array[EventCmd] = listView.listData.dropRight(1).toArray

  def newCmdDialog() = {
    val d = new NewEvtCmdBox(
      evtDiag, sm, owner, mapName, this,
      listView.selection.indices.headOption.map(_ + 1).getOrElse(0))
    d.open()
  }

  def editSelectedCmd() = {
    val selectedIdx = listView.selection.indices.head
    val selectedCmd = listView.selection.items.head
    val d = EventCmdDialog.dialogFor(owner, sm, mapName, selectedCmd,
      newEvt => {
        listView.listData = listView.listData.updated(selectedIdx, newEvt)
      })
    d.open()
  }

  contents += listView

  listenTo(listView.mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      if (e.peer.getButton() == MouseEvent.BUTTON3) {
        // List won't automatically select
        val closestI = listView.peer.locationToIndex(e.point)
        if (closestI != -1) {
          listView.selectIndices(closestI)

          val menu = new RpgPopupMenu {
            contents += new MenuItem(Action("Insert command") {
              newCmdDialog();
            })

            val selectedCmd = listView.listData(closestI)

            if (!selectedCmd.isInstanceOf[EndOfScript]) {
              contents += new MenuItem(Action("Edit") {
                editSelectedCmd();
              })
              contents += new MenuItem(Action("Delete") {
                if (!listView.selection.items.isEmpty) {
                  listView.listData = rpgboss.lib.Utils.removeFromSeq(
                    listView.listData, listView.selection.indices.head)
                }
              })
            }
          }

          menu.show(this, e.point.x, e.point.y)
        }
      } else if (e.clicks == 2) {
        newCmdDialog()
      }
  }

  /**
   * @param   idx   The index of the new command. Other elements will be moved
   *                to "fit" the new command in.
   */
  def insertCmd(idx: Int, cmd: EventCmd) = {
    val newList =
      listView.listData.take(idx) ++
      Seq(cmd) ++
      listView.listData.takeRight(listView.listData.length - idx)

    listView.listData = newList
  }
}