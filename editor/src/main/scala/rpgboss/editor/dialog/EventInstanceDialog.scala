package rpgboss.editor.dialog

import scala.swing._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._
import rpgboss.model.event._
import rpgboss.editor.uibase._
import scala.collection.mutable.ArrayBuffer
import scala.swing.TabbedPane.Page
import rpgboss.model._
import rpgboss.editor.StateMaster
import java.awt.Dimension
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.resourceselector.SpriteField
import javax.swing.BorderFactory
import rpgboss.lib.Utils
import rpgboss.model.MapLoc
import rpgboss.model.event.RpgEvent

class EventInstanceDialog(
  owner: Window,
  sm: StateMaster,
  initialEvent: RpgEvent,
  onOk: RpgEvent => Any,
  onCancel: RpgEvent => Any)
  extends StdDialog(owner, "Event Instance: " + initialEvent.name) {

  val model = Utils.deepCopy(initialEvent)

  override def cancelFunc() = onCancel(model)

  def okFunc() = {
    onOk(model)
    close()
  }

  val container = new BoxPanel(Orientation.Vertical) {
    def update() = {
      contents.clear()

      val eventClass =
        sm.getProjData.enums.eventClasses.apply(model.eventClassId)

      val freeVariableMap =
        collection.mutable.Map[String, EventParameter[_]]()
      val componentMap =
        collection.mutable.Map[String, Component]()

      for (state <- eventClass.states;
           cmd <- state.cmds;
           field <- EventParameterField.getParameterFields(sm.getProjData, cmd);
           if field.model.valueTypeId ==
             EventParameterValueType.LocalVariable.id) {
        val paramCopy = Utils.deepCopy(field.model)
        val component = field.constantComponentFactory(paramCopy)

        freeVariableMap.update(field.model.localVariable, paramCopy)
        componentMap.update(field.model.localVariable, component)
      }

      model.params = freeVariableMap.values.toArray[EventParameter[_]]

      contents += new DesignGridPanel {
        componentMap map {
          case (name, component) => row().grid(lbl(name)).add(component)
        }
      }

      revalidate()
      repaint()
      EventInstanceDialog.this.repaint()
    }

    update()
  }

  val fClassId = indexedCombo(
      sm.getProjData.enums.eventClasses,
      model.eventClassId,
      model.eventClassId = _,
      Some(() => {
        container.update()
      }))

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new DesignGridPanel {
      row().grid(lbl("Event Class:")).add(fClassId)
    }

    contents += container

    contents += new DesignGridPanel {
      addButtons(okBtn, cancelBtn)
    }
  }
}