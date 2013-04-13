package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.lib._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.tileset.metadata._

class ResourcesDialog(owner: Window, sm: StateMaster)
  extends StdDialog(owner, "Resources") {
  def okFunc() = {
    tilesetsMetadataPanel.save()
    close()
  }

  val tilesetsMetadataPanel = new TilesetsMetadataPanel(sm)

  val tabPane = new TabbedPane() {
    import TabbedPane._
    pages += new Page("Tilesets", tilesetsMetadataPanel)
  }

  contents = new DesignGridPanel {
    row().grid().add(tabPane)
    addButtons(cancelBtn, okBtn)
  }

}