package rpgboss.editor.misc

import scala.swing._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor._
import rpgboss.editor.misc.GraphicsUtils._
import rpgboss.editor.misc.SwingUtils._
import scala.swing.event._

class MapLocPanel(window: Window, sm: StateMaster, initialLoc: MapLoc)
  extends BoxPanel(Orientation.Vertical) {
  var loc: MapLoc = initialLoc

  val mapView = new MapView(window, sm, MapScales.default) {
    preferredSize = new Dimension(600, 600)
    maximumSize = new Dimension(600, 600)
    override def mousePressed(
      e: MousePressed,
      x0: Float,
      y0: Float,
      vs: MapViewState): Option[(Boolean, MouseFunction, MouseFunction)] = {

      //loc = MapLoc(vs.map.name, x0-0.5f, y0-0.5f) any loc
      loc = MapLoc(vs.map.name, x0.toInt + 0.5f, y0.toInt + 0.5f)
      curLocField.update()
      updateCursorSq(TileRect(x0.toInt, y0.toInt, 1, 1))

      Some((true, mousePressed(e, _, _, _), mousePressed(e, _, _, _)))
    }

    override def selectMap(mapOpt: Option[RpgMap]) = {
      viewStateOpt = mapOpt map { mapMeta =>
        new MapViewState(sm, mapMeta.name)
      }

      resizeRevalidateRepaint()

      updateCursorSq(TileRect.empty)

      mapOpt map { map =>
        if (map.name == loc.map) {
          updateCursorSq(TileRect(loc.x - 0.5f, loc.y - 0.5f))
          scrollPane.center(loc.x, loc.y)
        }
      }
    }
  }

  val selector = new MapSelector(sm) {
    override def onSelectMap(mapOpt: Option[RpgMap]) = {
      mapView.selectMap(mapOpt)
    }
  }

  // Initialize views
  selector.selectNode(selector.getNode(loc.map))

  val curLocField = new TextField {
    enabled = false

    def update() = {
      text = "Current location: %s".format(loc)
    }
    update()
  }

  contents += curLocField

  contents += new BoxPanel(Orientation.Horizontal) {
    contents += selector
    contents += mapView
  }
}