package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.editor.dialog._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog.cmd._
import rpgboss.model.event._
import rpgboss.model.Constants._
import rpgboss.model._
import rpgboss.editor.StateMaster
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.dialog.EventDialog
import rpgboss.editor.Internationalized._

class NewEventCmdBox(
  sm: StateMaster,
  owner: Window,
  eventLoc: Option[MapLoc],
  cmdBox: CommandBox,
  idxToInsert: Int)
  extends StdDialog(owner, getMessage("New_Command")) {

  centerDialog(new Dimension(400, 400))

  // Noop, as there is no okay button
  def okFunc() = {}

  def btnEvtCmd(title: String, e: EventCmd) = {
    new Button() {
      action = Action(title) ({
        val d = EventCmdDialog.dialogFor(
          owner,
          sm,
          eventLoc.map(_.map),
          e,
          evtCmd => {
            NewEventCmdBox.this.close()
            cmdBox.insertCmd(idxToInsert, evtCmd)
          })

        if (d != null) {
          d.open()
        } else {
          cmdBox.insertCmd(idxToInsert, e)
          NewEventCmdBox.this.close()
        }
      })
    }
  }

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        row().grid().add(leftLabel(getMessageColon("Windows")))
        row().grid().add(btnEvtCmd(getMessage("Show_Text"), ShowText()))
        row().grid().add(btnEvtCmd(getMessage("Get_Choice"), GetChoice()))
        row().grid().add(btnEvtCmd(getMessage("Show_Picture"), ShowPicture()))
        row().grid().add(btnEvtCmd(getMessage("Hide_Picture"), HidePicture()))
        row().grid().add(
            btnEvtCmd(getMessage("Tint_Screen"), TintScreen()))

        row().grid().add(leftLabel(getMessageColon("Movement")))
        row().grid().add(btnEvtCmd(getMessage("Teleport_Player"),
          Teleport(eventLoc.getOrElse(MapLoc()), Transitions.FADE.id)))
        row().grid().add(btnEvtCmd(getMessage("Move_Event"), MoveEvent()))
        row().grid().add(btnEvtCmd(getMessage("Lock_Player_Movement"),
          LockPlayerMovement(Array())))

        row().grid().add(leftLabel(getMessageColon("Party")))
        row().grid().add(btnEvtCmd(getMessage("Modify_Party"), ModifyParty()))
        row().grid().add(btnEvtCmd(getMessage("Heal_Damage_Party"), HealOrDamage()))

        row().grid().add(leftLabel(getMessageColon("Inventory")))
        row().grid().add(
            btnEvtCmd(getMessage("Add_Remove_Item"), AddRemoveItem()))
        row().grid().add(
            btnEvtCmd(getMessage("Add_Remove_Gold"), AddRemoveGold()))
        row().grid().add(btnEvtCmd(getMessage("Open_Store"), OpenStore()))
      }

      contents += new DesignGridPanel {
        row().grid().add(leftLabel(getMessage("Battles")))
        row().grid().add(btnEvtCmd(getMessage("Start_Battle"), StartBattle()))

        row().grid().add(leftLabel(getMessage("Audio")))
        row().grid().add(btnEvtCmd(getMessage("Play_Sound"), PlaySound()))
        row().grid().add(btnEvtCmd(getMessage("Play_Music"), PlayMusic()))
        row().grid().add(btnEvtCmd(getMessage("Stop_Sound"), StopMusic()))

        row().grid().add(leftLabel(getMessage("Programming")))
        row().grid().add(
            btnEvtCmd(getMessage("If_Condition"), IfCondition()))
        row().grid().add(
            btnEvtCmd(getMessage("While_Loop"), WhileLoop()))
        row().grid().add(
            btnEvtCmd(getMessage("Break_Loop"), BreakLoop()))
        row().grid().add(
            btnEvtCmd(getMessage("Set_Global_Integer"), SetGlobalInt()))
        row().grid().add(
            btnEvtCmd(getMessage("Change_Event_State"), SetEventState()))
        row().grid().add(btnEvtCmd(getMessage("Run_Javascript"), RunJs()))
      }

    }
    contents += new DesignGridPanel {
      addCancel(cancelBtn)
    }
  }

}