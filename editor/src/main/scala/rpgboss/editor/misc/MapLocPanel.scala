package rpgboss.editor.misc

import scala.swing._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor._
import rpgboss.editor.misc.GraphicsUtils._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing.event._

class MapLocPanel(window: Window, sm: StateMaster, initialLoc: MapLoc,
                  selectMapOnly: Boolean)
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

      if (!selectMapOnly) {
        //loc = MapLoc(vs.map.name, x0-0.5f, y0-0.5f) any loc
        loc = MapLoc(vs.map.name, x0.toInt + 0.5f, y0.toInt + 0.5f)
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
          // When selecting a map only, treat switching maps as changing the loc
          loc = MapLoc(map.name, 0, 0)
          curLocField.update()
        } else if (map.name == loc.map) {
          // Otherwise, center on the 'loc' when switched-to map contains it
          updateCursorSq(TileRect(loc.x - 0.5f, loc.y - 0.5f))
          scrollPane.center(loc.x, loc.y)
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
  selector.selectNode(selector.getNode(loc.map))

  val curLocField = new TextField {
    enabled = false

    def update() = {
      val displayId = sm.getMap(loc.map).get.displayId
      if (selectMapOnly)
        text = "Current location: %s".format(displayId)
      else
        text = "Current location: %s (%f, %f)".format(displayId, loc.x, loc.y)
    }
    update()
  }

  contents += curLocField

  contents += new BoxPanel(Orientation.Horizontal) {
    contents += selector
    contents += mapView
  }
}