package rpgboss.editor.dialog

import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.StateMaster
import rpgboss.editor.resourceselector.MusicField

class MapPropertiesDialog(
  owner: Window,
  sm: StateMaster,
  title: String,
  initialMap: RpgMap,
  initialMapData: RpgMapData,
  onOk: (RpgMap, RpgMapData) => Any)
  extends StdDialog(owner, title + " - " + initialMap.displayId) {

  var metadataModel = initialMap.metadata.copy()

  def okFunc(): Unit = {
    val newMap = initialMap.copy(metadata = metadataModel)

    val newMapData = {
      if (metadataModel.xSize == initialMap.metadata.xSize &&
        metadataModel.ySize == initialMap.metadata.ySize) {
        initialMapData
      } else {
        initialMapData.resized(metadataModel.xSize, metadataModel.ySize)
      }
    }

    onOk(newMap, newMapData)
    close()
  }

  val fieldTitle = textField(metadataModel.title, metadataModel.title = _)

  val fieldWidth = new NumberSpinner(
    metadataModel.xSize,
    RpgMap.minXSize, RpgMap.maxXSize,
    metadataModel.xSize = _)

  val fieldHeight = new NumberSpinner(
    initialMap.metadata.ySize,
    RpgMap.minYSize, RpgMap.maxYSize,
    metadataModel.ySize = _)

  val fieldChangeMusic = boolField(
    metadataModel.changeMusicOnEnter, 
    v => {
      metadataModel.changeMusicOnEnter = v
      fieldMusic.enabled = v
    },
    "Change music on enter")
    
  val fieldMusic = new MusicField(
    owner, sm, metadataModel.music,
    metadataModel.music = _)
  
  fieldMusic.enabled = metadataModel.changeMusicOnEnter

  contents = new DesignGridPanel {
    
    row().grid(leftLabel("Map ID:")).add(new TextField {
      text = initialMap.id
      enabled = false
    })
    
    row().grid(leftLabel("Map Title:")).add(fieldTitle)

    row().grid(leftLabel("Dimensions:"))
      .add(leftLabel("Width")).add(leftLabel("Height"))
    row().grid()
      .add(fieldWidth).add(fieldHeight)
    
    row().grid(leftLabel("Music:")).add(fieldChangeMusic)
    row().grid().add(fieldMusic)

    addButtons(cancelBtn, okBtn)
  }

}