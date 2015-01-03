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
import rpgboss.editor.misc.RandomEncounterSettingsPanel
import rpgboss.editor.Internationalized._
import rpgboss.editor.resourceselector.BattleBackgroundField

class MapPropertiesDialog(
  owner: Window,
  sm: StateMaster,
  title: String,
  initialMap: RpgMap,
  initialMapData: RpgMapData,
  onOk: (RpgMap, RpgMapData) => Any)
  extends StdDialog(owner, title + " - " + initialMap.displayId) {

  centerDialog(new Dimension(400, 400))

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
    RpgMap.minXSize, RpgMap.maxXSize,
    model.xSize,
    model.xSize = _)

  val fHeight = new NumberSpinner(
    RpgMap.minYSize, RpgMap.maxYSize,
    initialMap.metadata.ySize,
    model.ySize = _)

  val fMusic = new MusicField(
    owner, sm, model.music,
    model.music = _)

  val fBattleback = new BattleBackgroundField(
    owner, sm, model.battleBackground, model.battleBackground = _,
    allowNone = false)

  val fBattleMusic = new MusicField(
    owner, sm, model.battleMusic, model.battleMusic = _, allowNone = false)

  val fTilesets =
    new TilesetArrayField(owner, sm, model.tilesets, model.tilesets = _)

  val fRandomEncounters = new RandomEncounterSettingsPanel(
      owner, sm.getProjData, model.randomEncounterSettings)

  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        row()
          .grid(leftLabel(getMessageColon("Map_ID"))).add(new TextField {
            text = initialMap.id
            enabled = false
          })

        row().grid(leftLabel(getMessageColon("Map_Title"))).add(fTitle)

        row().grid(leftLabel(getMessageColon("Dimensions")))
          .add(leftLabel(getMessage("Width"))).add(leftLabel(getMessage("Height")))
        row().grid()
          .add(fWidth).add(fHeight)

        row().grid(leftLabel(getMessageColon("Music")))
          .add(fMusic)

        row().grid(leftLabel(needsTranslation("Battle Background:")))
          .add(fBattleback)
        row().grid(leftLabel(needsTranslation("Battle Music:")))
          .add(fBattleMusic)

        row().grid(leftLabel(getMessageColon("Tilesets"))).add(fTilesets)
      }

      contents += fRandomEncounters
    }

    contents += new DesignGridPanel {
      addButtons(okBtn, cancelBtn)
    }
  }
}