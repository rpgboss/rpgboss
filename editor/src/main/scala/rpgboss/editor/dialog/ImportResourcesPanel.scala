package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import scala.swing.event._

class ImportResourcesPanel(sm: StateMaster)
  extends GridPanel(1, 3) {

  case class ResourceItem(custom: Boolean, name: String)

  val resourceTypeList = new ListView(Resource.resourceTypes) {
    renderer = ListView.Renderer.apply(_.rcType)

    listenTo(selection)
    reactions += {
      case ListSelectionChanged(_, _, _) =>
        updateResourceFileList()
    }
  }

  val resourceFileList = new ListView[ResourceItem] 
    with IsEnhancedListView[ResourceItem] {
    renderer = ListView.Renderer.apply(item => {
      if (item.custom)
        item.name
      else
        "%s (built-in)".format(item.name)
    })

    listenTo(selection)
    reactions += {
      case ListSelectionChanged(_, _, _) =>
        btnDelete.enabled = 
          selection.items.headOption.map(_.custom).getOrElse(false)
    }
  }

  val btnImport = new Button(Action("Import...") {
    val metaResource = resourceTypeList.selection.items.head

    val chooser = new FileChooser {
      fileSelectionMode = FileChooser.SelectionMode.FilesOnly
      multiSelectionEnabled = true
      fileFilter = {
        new javax.swing.filechooser.FileNameExtensionFilter(
          metaResource.keyExts.mkString(", "), metaResource.keyExts : _*)
      }
    }

    val result = chooser.showDialog(this, "Import")
    if (result == FileChooser.Result.Approve) {
      for (file <- chooser.selectedFiles) {
        metaResource.importCustom(sm.getProj, file)
      }

      updateResourceFileList()
    }
  })

  val btnDelete: Button = new Button(Action("Delete...") {
    val selection = resourceFileList.selection.items.head
    assert(selection.custom)

    val result = Dialog.showConfirmation(
      this,
      "Are you sure you want to delete custom resource: %s?".format(
        selection.name),
      "Delete resource")

    if (result == Dialog.Result.Yes) {
      val metaResource = resourceTypeList.selection.items.head
      metaResource.deleteCustom(sm.getProj, selection.name)
      
      updateResourceFileList()
    }
  }) {
    enabled = false
  }

  resourceTypeList.selectIndices(0)

  def updateResourceFileList(): Unit = {
    val resourceType = resourceTypeList.selection.items.head

    val resourceList = new collection.mutable.ArrayBuffer[ResourceItem]
    for (name <- resourceType.listSystemResources()) {
      resourceList += ResourceItem(false, name)
    }
    for (name <- resourceType.listCustomResources(sm.getProj)) {
      resourceList += ResourceItem(true, name)
    }

    resourceFileList.updatePreserveSelection(resourceList)
  }

  import SwingUtils._
  contents += new DesignGridPanel {
    row().grid().add(lbl("Resource type:"))
    row().grid().add(new ScrollPane {
      contents = resourceTypeList
    })
  }

  contents += new DesignGridPanel {
    row().grid().add(lbl("Files:"))
    row().grid().add(new ScrollPane {
      contents = resourceFileList
    })
  }

  contents += new DesignGridPanel {
    row.grid().add(btnImport)
    row.grid().add(btnDelete)
  }
}