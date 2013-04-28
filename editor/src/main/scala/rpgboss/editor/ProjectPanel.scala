package rpgboss.editor

import rpgboss.editor.uibase._
import scala.collection.JavaConversions._
import rpgboss.editor.imageset.selector._
import rpgboss.editor.misc._
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.LifecycleListener
import org.lwjgl.opengl.Display

class ProjectPanel(val mainP: MainPanel, sm: StateMaster)
  extends BorderPanel
  with SelectsMap {
  val tileSelector = new TabbedTileSelector(sm)
  val mapSelector = new ProjectPanelMapSelector(sm, this)
  val mapView = new MapEditor(this, sm, tileSelector)

  val projMenu = new PopupMenu {
    contents += new MenuItem(mainP.actionNew)
    contents += new MenuItem(mainP.actionOpen)
    contents += new MenuItem(mainP.actionSave)
  }

  def selectMap(mapOpt: Option[RpgMap]) = {
    List(tileSelector, mapView).map(_.selectMap(mapOpt))
  }

  val topBar = new BoxPanel(Orientation.Horizontal) {
    import rpgboss.editor.dialog._

    contents += new Button {
      val btn = this
      action = Action("Project \u25BC") {
        projMenu.show(btn, 0, btn.bounds.height)
      }
    }
    contents += new Button(Action("Database...") {
      val d = new DatabaseDialog(mainP.topWin, sm)
      d.open()
    })
    contents += new Button(Action("Resources...") {
      val d = new ResourcesDialog(mainP.topWin, sm)
      d.open()
    })
    contents += new Button(Action("Play...") {
      if (sm.askSaveUnchanged(this)) {
        val separator = System.getProperty("file.separator")

        val classpath =
          List("java.class.path", "java.boot.class.path", "sun.boot.class.path")
            .map(s => System.getProperty(s, "")).mkString(":")

        println(classpath)

        val javaPath =
          System.getProperty("java.home") +
            separator +
            "bin" +
            separator +
            "java";

        val projPath = sm.getProj.dir.getCanonicalPath()

        val processBuilder =
          new ProcessBuilder(javaPath, "-cp",
            classpath,
            "rpgboss.player.PlayerDesktop",
            projPath)

        println(processBuilder.command().mkString(" "))
        val process = processBuilder.start();
        process.waitFor();

      }
    })
  }

  val sidePane =
    new SplitPane(Orientation.Horizontal, tileSelector, mapSelector) {
      resizeWeight = 1.0
    }

  layout(mapView) = BorderPanel.Position.Center
  layout(sidePane) = BorderPanel.Position.West
  layout(topBar) = BorderPanel.Position.North

  // select most recent or first map if not empty
  val initialMap = {
    val mapStates = sm.getMapStates
    if (!mapStates.isEmpty) {
      val idToLoad =
        if (mapStates.contains(sm.getProj.data.recentMapName))
          sm.getProj.data.recentMapName
        else
          mapStates.keys.min

      mapStates.get(idToLoad).map(_.map)
    } else None
  }

  // This calls the selectMapFunction
  selectMap(initialMap)
  initialMap.map(m =>
    mapSelector.highlightWithoutEvent(mapSelector.getNode(m.name)))

  mainP.revalidate()
}

