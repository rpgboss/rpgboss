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
import javax.swing.UIManager
import javax.swing.BorderFactory
import java.awt.Font

/**
 * @param   onUpdateCmd   Called when the command at index is updated with a
 *                        new version.
 */
class CommandBoxListViewRenderer(
  evtDiag: EventDialog,
  owner: Window,
  sm: StateMaster,
  mapName: String,
  onUpdateCmd: (Int, EventCmd) => Unit)
  extends ListView.Renderer[EventCmd] {
  def componentFor(list: ListView[_], isSelected: Boolean, hasFocus: Boolean,
      cmd: EventCmd, index: Int) = new BoxPanel(Orientation.Vertical) {

    val evtCmdColor =
      if (isSelected)
        UIManager.getColor("TextArea.selectionBackground")
      else
        UIManager.getColor("TextArea.inactiveBackground")

    background = evtCmdColor

    val textFont = new Font("Monospaced", Font.BOLD, 14)

    cmd.sections.zipWithIndex.foreach {
      case (EventCmd.PlainLines(lines), i) => {
        contents += new BoxPanel(Orientation.Horizontal) {
          background = evtCmdColor
          contents += new Label {
            font = textFont
            xAlignment = Alignment.Left

            val beginBlurb = if (i == 0) "&gt;&gt;&gt; " else "... "
            text = "<html>" + beginBlurb +
                   lines
                     .map(_.replace(" ", "&nbsp;"))
                     .mkString("<br>... ") +
                   "</html>"
          }
          contents += Swing.HGlue
        }
      }
      case (EventCmd.CommandList(innerCmds, indent), sectionI) => {
        contents += new BoxPanel(Orientation.Horizontal) {
          background = evtCmdColor

          val textFontMetrics =
            evtDiag.peer.getGraphics().getFontMetrics(textFont)
          val indentPx = textFontMetrics.stringWidth(("  " * indent))
          contents += Swing.HStrut(indentPx)

          def updateInnerCmds(newInnerCmds: Array[EventCmd]) =
            onUpdateCmd(index, cmd.copyWithNewInnerCmds(sectionI, newInnerCmds))

          contents += new CommandBox(evtDiag, owner, sm, mapName, innerCmds,
                                     updateInnerCmds)
          contents += Swing.HGlue
        }
      }
    }
  }
}

/**
 * Holds and edits list of commands.
 * Current state is held in listData member.
 * @param     onUpdate    Called when the contents change.
 */
class CommandBox(
  evtDiag: EventDialog,
  owner: Window,
  sm: StateMaster,
  mapName: String,
  initialCmds: Array[EventCmd],
  onUpdate: (Array[EventCmd]) => Unit)
  extends GridPanel(1, 1) {

  val listView = new ListView[EventCmd] {
    listData = initialCmds ++ List(EndOfScript())

    renderer =
      new CommandBoxListViewRenderer(evtDiag, owner, sm, mapName, updateCmd)
  }

  def getEventCmds: Array[EventCmd] = listView.listData.dropRight(1).toArray

  def newCmdDialog() = {
    val iToInsert =
      if (!listView.selection.indices.headOption.isDefined) {
        0
      } else {
        val selectedI = listView.selection.indices.head

        // Insert after the selected EvtCmd in all cases unless it is the
        // EndOfScript sentinel EventCmd at the very end.
        if (selectedI == listView.listData.length - 1)
          selectedI
        else
          selectedI + 1
      }

    val d = new NewEvtCmdBox(evtDiag, sm, owner, mapName, this, iToInsert)
    d.open()
  }

  def editSelectedCmd() = {
    val selectedIdx = listView.selection.indices.head
    val selectedCmd = listView.selection.items.head
    val d = EventCmdDialog.dialogFor(owner, sm, mapName, selectedCmd,
      newEvt => updateCmd(selectedIdx, newEvt))
    if (d != null)
      d.open()
    else
      Dialog.showMessage(this, "Nothing to edit", "Info")
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
            contents += new MenuItem(Action("Insert command...") {
              newCmdDialog();
            })

            val selectedCmd = listView.listData(closestI)

            if (!selectedCmd.isInstanceOf[EndOfScript]) {
              contents += new MenuItem(Action("Edit...") {
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

  def updateCmd(idx: Int, cmd: EventCmd): Unit = {
    listView.listData = listView.listData.updated(idx, cmd)
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