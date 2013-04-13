package rpgboss.editor.dialog

import rpgboss.editor.dialog.db._
import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class DatabaseDialog(owner: Window, sm: StateMaster)
  extends StdDialog(owner, "Database") {
  var model = sm.getProj.data

  val charPane = new CharactersPanel(owner, sm, this)
  val classesPane = new ClassesPanel(owner, sm, this)
  val itemsPane = new ItemsPanel(owner, sm, this)
  val statusPane = new StatusPanel(owner, sm, this)
  val sysPane = new SystemPanel(owner, sm, this)
  val enumPane = new EnumerationsPanel(owner, sm, this)

  val panels =
    List(charPane, classesPane, itemsPane, statusPane, sysPane, enumPane)

  def applyFunc() = {
    val newProj = sm.getProj.copy(data = model)

    sm.setProj(newProj)
  }

  def okFunc() = {
    applyFunc()
    close()
  }

  val tabPane = new TabbedPane() {
    import TabbedPane._

    panels.foreach { panel =>
      pages += new Page(panel.panelName, panel)
    }

    listenTo(selection)

    reactions += {
      case SelectionChanged(pane) =>
        selection.page.content.asInstanceOf[DatabasePanel].activate()
    }
  }

  lazy val applyBtn = new Button(new Action("Apply") {
    def apply() = {
      applyFunc()
    }
  })

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(cancelBtn, okBtn, Some(applyBtn))
  }

}