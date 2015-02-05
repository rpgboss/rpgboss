package rpgboss.editor.misc

import scala.swing._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor._
import rpgboss.editor.misc.GraphicsUtils._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._

class MapLocPanel(window: Window, sm: StateMaster, model: MapLoc,
                  selectMapOnly: Boolean)
  extends BoxPanel(Orientation.Vertical) {

  val mapView = new MapView(window, sm, MapScales.default) {
    preferredSize = new Dimension(600, 600)
    maximumSize = new Dimension(600, 600)
    override def mousePressed(
      e: MousePressed,
      x0: Float,
      y0: Float,
      vs: MapViewState): Option[(Boolean, MouseFunction, MouseFunction)] = {

      if (!selectMapOnly) {
        model.map = vs.map.name
        model.x = x0.toInt + 0.5f
        model.y = y0.toInt + 0.5f
        curLocField.update()
        updateCursorSq(TileRect(x0.toInt, y0.toInt, 1, 1))
      }

      Some((true, mousePressed(e, _, _, _), mousePressed(e, _, _, _)))
    }

    // Re-acquires onto loc if selected map contains loc
    override def selectMap(mapOpt: Option[RpgMap]) = {
      viewStateOpt = mapOpt map { mapMeta =>
        new MapViewState(sm, mapMeta.name)
      }

      updateCursorSq(TileRect.empty)

      mapOpt map { map =>
        if (selectMapOnly) {
          model.map = map.name
          curLocField.update()
        } else if (map.name == model.map) {
          // Otherwise, center on the 'loc' when switched-to map contains it
          updateCursorSq(TileRect(model.x - 0.5f, model.y - 0.5f))
          scrollPane.center(model.x, model.y)
        }
      }

      resizeRevalidateRepaint()
    }
  }

  val selector = new MapSelector(sm) {
    override def onSelectMap(mapOpt: Option[RpgMap]) = {
      mapView.selectMap(mapOpt)
    }
  }

  // Initialize views
  for (node <- selector.getNode(model.map))
    selector.selectNode(node)

  val curLocField = new TextField {
    enabled = false

    def update() = {
      sm.getMap(model.map) map { map =>
        val displayId = map.displayId
        if (selectMapOnly)
          text = "Current location: %s".format(displayId)
        else
          text = "Current location: %s (%f, %f)".format(
              displayId, model.x, model.y)
      } getOrElse {
        text = "No map selected"
      }
    }
    update()
  }

  contents += curLocField

  contents += new BoxPanel(Orientation.Horizontal) {
    contents += selector
    contents += mapView
  }
}