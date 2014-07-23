package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.uibase._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.imageset.metadata._
import rpgboss.editor.uibase.StdDialog

class ResourcesDialog(owner: Window, sm: StateMaster)
  extends StdDialog(owner, "Resources") {
  def okFunc() = {
    tilesetsMetadataPanel.save()
    close()
  }

  val importResourcesPanel = new ImportResourcesPanel(sm)
  val tilesetsMetadataPanel = new TilesetsMetadataPanel(sm)

  val tabPane = new TabbedPane() {
    import TabbedPane._
    pages += new Page("Import", importResourcesPanel)
    pages += new Page("Tilesets", tilesetsMetadataPanel)
  }

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(cancelBtn, okBtn)
  }
}
