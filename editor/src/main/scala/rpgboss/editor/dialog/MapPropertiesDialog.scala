package rpgboss.editor.dialog

import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import rpgboss.editor.uibase._
import rpgboss.editor.misc.SwingUtils._

class MapPropertiesDialog(
  owner: Window,
  title: String,
  initialMap: RpgMap,
  initialMapData: RpgMapData,
  onOk: (RpgMap, RpgMapData) => Any)
  extends StdDialog(owner, title + " - " + initialMap.displayId) {

  def okFunc(): Unit = {
    val newMap = initialMap.copy(
      metadata =
        initialMap.metadata.copy(
          title = fieldTitle.text,
          xSize = fieldWidth.getValue,
          ySize = fieldHeight.getValue))

    val newMapData = {
      if (fieldWidth.getValue == initialMap.metadata.xSize &&
        fieldHeight.getValue == initialMap.metadata.ySize) {
        initialMapData
      } else {
        initialMapData.resized(fieldWidth.getValue, fieldHeight.getValue)
      }
    }

    onOk(newMap, newMapData)
    close()
  }

  val fieldTitle = new TextField {
    text = initialMap.metadata.title
  }

  val fieldWidth = new NumberSpinner(
    initialMap.metadata.xSize,
    RpgMap.minXSize, RpgMap.maxXSize)

  val fieldHeight = new NumberSpinner(
    initialMap.metadata.ySize,
    RpgMap.minYSize, RpgMap.maxYSize)

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Map ID:"), 2)
    row().grid().add(new TextField {
      text = initialMap.id
      enabled = false
    }, 2)
    row().grid().add(leftLabel("Map Title:"), 2)
    row().grid().add(fieldTitle, 2)

    row().grid().add(leftLabel("Width:")).add(leftLabel("Height:"))
    row().grid().add(fieldWidth).add(fieldHeight)

    addButtons(cancelBtn, okBtn)
  }

}