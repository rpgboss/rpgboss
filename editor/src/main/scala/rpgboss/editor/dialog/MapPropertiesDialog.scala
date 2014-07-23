package rpgboss.editor.dialog

import rpgboss.model._
import rpgboss.model.resource._
import scala.swing._
import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.StateMaster
import rpgboss.editor.resourceselector.MusicField
import rpgboss.editor.resourceselector.TilesetArrayField
import rpgboss.editor.resourceselector.TilesetArrayField

class MapPropertiesDialog(
  owner: Window,
  sm: StateMaster,
  title: String,
  initialMap: RpgMap,
  initialMapData: RpgMapData,
  onOk: (RpgMap, RpgMapData) => Any)
  extends StdDialog(owner, title + " - " + initialMap.displayId) {

  val model = initialMap.metadata.copy()

  def okFunc(): Unit = {
    val newMap = initialMap.copy(metadata = model)

    val newMapData = {
      if (model.xSize == initialMap.metadata.xSize &&
        model.ySize == initialMap.metadata.ySize) {
        initialMapData
      } else {
        initialMapData.resized(model.xSize, model.ySize)
      }
    }

    onOk(newMap, newMapData)
    close()
  }

  val fTitle = textField(model.title, model.title = _)

  val fWidth = new NumberSpinner(
    model.xSize,
    RpgMap.minXSize, RpgMap.maxXSize,
    model.xSize = _)

  val fHeight = new NumberSpinner(
    initialMap.metadata.ySize,
    RpgMap.minYSize, RpgMap.maxYSize,
    model.ySize = _)

  val fChangeMusic = boolField(
    "Change music on enter",
    model.changeMusicOnEnter,
    model.changeMusicOnEnter = _,
    Some(setEnabledFields))

  val fMusic = new MusicField(
    owner, sm, model.music,
    model.music = _)

  val fTilesets =
    new TilesetArrayField(owner, sm, model.tilesets, model.tilesets = _)

  def setEnabledFields() =
    fMusic.enabled = model.changeMusicOnEnter

  setEnabledFields()

  contents = new DesignGridPanel {

    row().grid(leftLabel("Map ID:")).add(new TextField {
      text = initialMap.id
      enabled = false
    })

    row().grid(leftLabel("Map Title:")).add(fTitle)

    row().grid(leftLabel("Dimensions:"))
      .add(leftLabel("Width")).add(leftLabel("Height"))
    row().grid()
      .add(fWidth).add(fHeight)

    row().grid(leftLabel("Music:")).add(fChangeMusic)
    row().grid().add(fMusic)

    row().grid(leftLabel("Tilesets:")).add(fTilesets)

    addButtons(cancelBtn, okBtn)
  }

}