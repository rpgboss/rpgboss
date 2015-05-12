package rpgboss.editor.dialog

import rpgboss.editor.dialog.db._
import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.Internationalized._

import java.awt.Toolkit

class DatabaseDialog(owner: Window, sm: StateMaster)
  extends StdDialog(owner, getMessage("Database")) {

  val screenSize = Toolkit.getDefaultToolkit().getScreenSize();
  centerDialog(new Dimension(
      math.max(1024, screenSize.width / 2),
      math.max(600, screenSize.height / 2)))

  val model = Utils.deepCopy(sm.getProjData)

  val animationsPane = new AnimationsPanel(owner, sm, this)
  val charPane = new CharactersPanel(owner, sm, this)
  val classesPane = new ClassesPanel(owner, sm, this)
  val enemiesPane = new EnemiesPanel(owner, sm, this)
  val encountersPane = new EncountersPanel(owner, sm, this)
  val itemsPane = new ItemsPanel(owner, sm, this)
  val skillsPane = new SkillsPanel(owner, sm, this)
  val statusPane = new StatusPanel(owner, sm, this)
  val eventClassesPane = new EventClassesPanel(owner, sm, this)
  val sysPane = new SystemPanel(owner, sm, this)
  val enumPane = new EnumerationsPanel(owner, sm, this)
  val messagesPane = new MessagesPanel(owner, sm, this)
  val vehiclesPane = new VehiclesPane(owner, sm, this)

  val panels =
    List(charPane, classesPane, enemiesPane, encountersPane, itemsPane,
         skillsPane, statusPane, animationsPane, eventClassesPane, sysPane,
         enumPane, messagesPane, vehiclesPane)

  def applyFunc() = {
    sm.setProjData(model)
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

  lazy val applyBtn = new Button(new Action(getMessage("Apply")) {
    def apply() = {
      applyFunc()
    }
  })

  override def onClose() = {
    panels.foreach(_.dispose())
    super.onClose()
  }

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(okBtn, cancelBtn, Some(applyBtn))
  }

}