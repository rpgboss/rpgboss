package rpgboss.editor.resourceselector

import rpgboss.editor._
import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.editor.imageset.selector.TilesetTileSelector
import rpgboss.editor.uibase.IsEnhancedListView
import rpgboss.editor.uibase.DesignGridPanel
import scala.swing.event.MouseClicked

class TilesetArrayField(
  owner: Window,
  sm: StateMaster,
  initial: Array[String],
  onUpdate: (Array[String]) => Unit) extends DesignGridPanel {

  def editModel(initial: String, onUpdate: String => Unit) = {
    val d = new TilesetSelectDialog(owner, sm, Some(initial)) {
      override def onSuccess(result: Option[String]) = {
        result.map(onUpdate)
      }
    }
    d.open()
  }

  val fTilesets = new ListView(initial) with IsEnhancedListView[String] {
    listenTo(mouse.clicks)
    reactions += {
      case e: MouseClicked if e.clicks == 2 =>
        selection.indices.headOption.map { idx =>
          editModel(
            listData(idx),
            newVal => updatePreserveSelection(idx, newVal))
        }
    }

    override def onListDataUpdate() = {
      onUpdate(listData.toArray)
    }
  }

  val btnAdd = new Button(Action("Add...") {
    editModel("", newVal => {
      fTilesets.updatePreserveSelection(fTilesets.listData :+ newVal)
    })
  })

  val btnDelete = new Button(Action("Remove last") {
    fTilesets.updatePreserveSelection(fTilesets.listData.dropRight(1))
  })

  row().grid().add(new ScrollPane {
    contents = fTilesets
  }, 2)
  row().grid().add(btnAdd).add(btnDelete)
}

abstract class TilesetSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[String])
  extends ResourceSelectDialog(owner, sm, initial, allowNone = false, Tileset) {

  override def specToResourceName(spec: String): String = spec

  override def newRcNameToSpec(name: String, prevSpec: Option[String]): String =
    name

  override def rightPaneFor(
    selection: String,
    updateSelectionF: String => Unit): ResourceRightPane = {
    if (selection.isEmpty()) {
      return new Label("No Tileset selected") with ResourceRightPane
    }

    val tileset = Tileset.readFromDisk(sm.getProj, selection)
    new TilesetTileSelector(0, tileset, _ => Unit) with ResourceRightPane
  }
}