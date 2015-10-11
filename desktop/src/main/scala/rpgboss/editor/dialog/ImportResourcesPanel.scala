package rpgboss.editor.dialog

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.Internationalized._ 

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

  val btnImport = new Button(Action(getMessage("Import")) {
    val metaResource = resourceTypeList.selection.items.head

    val chooser = new FileChooser {
      fileSelectionMode = FileChooser.SelectionMode.FilesOnly
      multiSelectionEnabled = true
      fileFilter = {
        new javax.swing.filechooser.FileNameExtensionFilter(
          metaResource.keyExts.mkString(", "), metaResource.keyExts : _*)
      }
    }

    val result = chooser.showDialog(this, getMessage("Import"))
    if (result == FileChooser.Result.Approve) {
      for (file <- chooser.selectedFiles) {
        metaResource.importCustom(sm.getProj, file)
      }

      updateResourceFileList()
    }
  })

  val btnDelete: Button = new Button(Action(getMessage("Delete")) {
    val selection = resourceFileList.selection.items.head
    assert(selection.custom)

    val result = Dialog.showConfirmation(
      this,
      getMessage("Are_You_Sure_You_Want_To_Delete_Custom_Resource") + " %s?".format(
        selection.name),
      getMessage("Delete_Resource"))

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
    row().grid().add(lbl(getMessageColon("Resource_Type")))
    row().grid().add(new ScrollPane {
      contents = resourceTypeList
    })
  }

  contents += new DesignGridPanel {
    row().grid().add(lbl(getMessageColon("Files")))
    row().grid().add(new ScrollPane {
      contents = resourceFileList
    })
  }

  contents += new DesignGridPanel {
    row.grid().add(btnImport)
    row.grid().add(btnDelete)
  }
}