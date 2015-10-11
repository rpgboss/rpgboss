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
import rpgboss.editor.Internationalized._ 

class ResourcesDialog(owner: Window, sm: StateMaster)
  extends StdDialog(owner, getMessage("Resources")) {
  def okFunc() = {
    tilesetsMetadataPanel.save()
    close()
  }

  centerDialog(new Dimension(1200, 730))

  val importResourcesPanel = new ImportResourcesPanel(sm)
  val tilesetsMetadataPanel = new TilesetsMetadataPanel(sm)

  val tabPane = new TabbedPane() {
    import TabbedPane._
    pages += new Page(getMessage("Import"), importResourcesPanel)
    pages += new Page(getMessage("Tilesets"), tilesetsMetadataPanel)
  }

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(okBtn, cancelBtn)
  }
}
