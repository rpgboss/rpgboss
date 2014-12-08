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
import scala.collection.mutable.ArrayBuffer
import rpgboss.editor.Internationalized._ 

object EventCmdPanel {
  val textFont = new Font("Monospaced", Font.BOLD, 14)
}

class EventCmdPanel(
  owner: Window,
  sm: StateMaster,
  eventLoc: Option[MapLoc],
  parentCmdBox: CommandBox,
  initialIndex: Int,
  initial: EventCmd,
  onUpdate: (EventCmd) => Unit)
  extends BoxPanel(Orientation.Vertical) {

  var index = initialIndex

  background = UIManager.getColor("TextArea.background")

  initial.sections.zipWithIndex.foreach {
    case (PlainLines(lines), i) => {
      contents += new BoxPanel(Orientation.Horizontal) {
        opaque = false
        contents += new Label {
          font = EventCmdPanel.textFont
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
        opaque = true
        // TODO: Pick a better color
        //background = UIManager.getColor("TextArea.inactiveBackground")
        background = java.awt.Color.GRAY

        val indentPx = 20 * indent
        contents += Swing.RigidBox(new Dimension(indentPx, 1))

        def updateInnerCmds(newInnerCmds: Array[EventCmd]) =
          onUpdate(initial.copyWithNewInnerCmds(sectionI, newInnerCmds))

        contents += new CommandBox(owner, sm, eventLoc, innerCmds,
                                   updateInnerCmds, inner = true)
      }
    }
  }

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      requestFocus()
      if (e.peer.getButton() == MouseEvent.BUTTON3) {
        val menu = new RpgPopupMenu {
          contents += new MenuItem(Action(getMessage("Insert_Command_Above") + "...") {
            parentCmdBox.newCmdDialog(index)
          })
          contents += new MenuItem(Action(getMessage("Edit") + "...") {
            parentCmdBox.editSelectedCmd(index)
          })
          contents += new MenuItem(Action(getMessage("Delete")) {
            parentCmdBox.deleteCmd(index)
          })
        }

        menu.show(this, e.point.x, e.point.y)
      } else if (e.clicks == 2) {
        parentCmdBox.editSelectedCmd(index)
      }
  }

  focusable = true
  listenTo(this)
  reactions += {
    case e: FocusEvent if !e.temporary => {
      if (hasFocus)
        background = UIManager.getColor("TextArea.selectionBackground")
      else
        background = UIManager.getColor("TextArea.background")
    }
  }
}

/**
 * Holds and edits list of commands.
 * Current state is held in listData member.
 * @param     onUpdate    Called when the contents change.
 */
class CommandBox(
  owner: Window,
  sm: StateMaster,
  eventLoc: Option[MapLoc],
  initialCmds: Array[EventCmd],
  onUpdate: (Array[EventCmd]) => Unit,
  inner: Boolean)
  extends BoxPanel(Orientation.Vertical) {

  background = UIManager.getColor("TextArea.background")

  val model = ArrayBuffer(initialCmds : _*)
  def getEventCmds: Array[EventCmd] = model.toArray

  def newEventCmdPanel(i: Int, cmd: EventCmd) =
    new EventCmdPanel(owner, sm, eventLoc, this, i, cmd,
                      newCmd => updateCmd(i, newCmd))
  for ((cmd, i) <- model.zipWithIndex) {
    contents += newEventCmdPanel(i, cmd)
  }

  // The 'dummy' command to indicate to user where to place the next one.
  contents += new BoxPanel(Orientation.Horizontal) {
    background = UIManager.getColor("TextArea.background")
    contents += new Label(">>>") {
      font = EventCmdPanel.textFont
      xAlignment = Alignment.Left
    }
    contents += Swing.HGlue

    listenTo(this)
    listenTo(this.mouse.clicks)
    reactions += {
      case e: MouseClicked =>
        requestFocus()
        if (e.peer.getButton() == MouseEvent.BUTTON3) {
          val menu = new RpgPopupMenu {
            contents += new MenuItem(Action(getMessage("Insert_Command") + "...") {
              newCmdDialog(model.length)
            })
          }

          menu.show(this, e.point.x, e.point.y)
        } else if (e.clicks == 2) {
          newCmdDialog(model.length)
        }
      case e: FocusEvent if !e.temporary =>
        if (hasFocus)
          background = UIManager.getColor("TextArea.selectionBackground")
        else
          background = UIManager.getColor("TextArea.background")
    }
  }

  if (!inner)
    contents += Swing.VGlue

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked =>
      if (e.peer.getButton() == MouseEvent.BUTTON3) {
        val menu = new RpgPopupMenu {
          contents += new MenuItem(Action(getMessage("Insert_Command") + "...") {
            newCmdDialog(model.length)
          })
        }

        menu.show(this, e.point.x, e.point.y)
      } else if (e.clicks == 2) {
        newCmdDialog(model.length)
      }
  }

  def newCmdDialog(indexToInsert: Int) = {
    assert(indexToInsert >= 0)
    assert(indexToInsert <= model.length)
    val d = new NewEvtCmdBox(sm, owner, eventLoc, this, indexToInsert)
    d.open()
  }

  def editSelectedCmd(index: Int) = {
    assert(index >= 0)
    assert(index < model.length)
    val selectedCmd = model(index)
    val d = EventCmdDialog.dialogFor(owner, sm, eventLoc.map(_.map),
        selectedCmd, newEvt => updateCmd(index, newEvt))
    if (d != null)
      d.open()
    else
      Dialog.showMessage(this, getMessage("Nothing_To_Edit"), getMessage("Info"))
  }

  def insertCmd(idx: Int, cmd: EventCmd) = {
    model.insert(idx, cmd)
    onUpdate(model.toArray)

    // Insert a new panel.
    contents.insert(idx, newEventCmdPanel(idx, cmd))
    // Update the index of all the event panels following this one.
    for (i <- (idx + 1) until model.length) {
      contents(i).asInstanceOf[EventCmdPanel].index += 1
    }
    revalidate()
  }

  def updateCmd(idx: Int, cmd: EventCmd): Unit = {
    model.update(idx, cmd)
    onUpdate(model.toArray)

    // Insert a new panel.
    contents.update(idx, newEventCmdPanel(idx, cmd))
    revalidate()
  }

  def deleteCmd(idx: Int) = {
    assert(idx >= 0)
    assert(idx < model.length)
    model.remove(idx)
    onUpdate(model.toArray)

    contents.remove(idx)
    for (i <- idx until model.length) {
      contents(i).asInstanceOf[EventCmdPanel].index -= 1
    }
    revalidate()
  }
}