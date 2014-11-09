package rpgboss.editor

import rpgboss.editor.uibase._
import scala.collection.JavaConversions._
import rpgboss.editor.imageset.selector._
import rpgboss.editor.misc._
import scala.swing._
import scala.swing.event._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.LifecycleListener
import org.lwjgl.opengl.Display
import java.io._
import java.util.Scanner
import javax.swing.ImageIcon
import rpgboss.editor.util.Export
import java.awt.Desktop
import java.awt.Toolkit

class ProjectPanel(val mainP: MainPanel, sm: StateMaster)
  extends BorderPanel
  with SelectsMap {
  val tileSelector = new TabbedTileSelector(sm)
  val mapSelector = new ProjectPanelMapSelector(sm, this)
  val mapView = new MapEditor(this, sm, tileSelector)

  val window = mainP.getWindow()
  window.resizable = true
  window.location = new Point(0,0)

  val screenSize = Toolkit.getDefaultToolkit().getScreenSize();

  window.minimumSize = new Dimension(screenSize.width/2,screenSize.height/2)
  window.maximize()

  val projMenu = new PopupMenu {
    contents += new MenuItem(mainP.actionNew)
    contents += new MenuItem(mainP.actionOpen)
    contents += new MenuItem(mainP.actionSave)
    contents += new MenuItem(mainP.actionClose)
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
      icon = new ImageIcon(Utils.readClasspathImage(
        "crystal_project/16x16/devices/blockdevice.png"))
    }
    contents += new Button(Action("Database...") {
      val d = new DatabaseDialog(mainP.topWin, sm)
      d.open()
    }) {
      icon = new ImageIcon(Utils.readClasspathImage(
        "crystal_project/16x16/apps/database.png"))
    }
    contents += new Button(Action("Resources...") {
      val d = new ResourcesDialog(mainP.topWin, sm)
      d.open()
    }) {
      icon = new ImageIcon(Utils.readClasspathImage(
        "crystal_project/16x16/filesystems/folder_images.png"))
    }
    contents += new Button(Action("Play...") {
      if (sm.askSaveUnchanged(this)) {
        def inheritIO(src: InputStream, dest: PrintStream) = {
          new Thread(new Runnable() {
              def run() = {
                  val sc = new Scanner(src);
                  while (sc.hasNextLine()) {
                      dest.println(sc.nextLine());
                  }
              }
          }).start();
        }

        val projPath = sm.getProj.dir.getCanonicalPath()

        val processBuilder: ProcessBuilder = {
          val separator = System.getProperty("file.separator")
          val cpSeparator = System.getProperty("path.separator")
          val classpath =
            List("java.class.path", "java.boot.class.path",
                 "sun.boot.class.path")
              .map(s => System.getProperty(s, "")).mkString(cpSeparator)

          val javaPath =
            System.getProperty("java.home") +
              separator +
              "bin" +
              separator +
              "java";

          new ProcessBuilder(javaPath, "-cp",
            classpath,
            "rpgboss.editor.RpgDesktop",
            "--player",
            projPath)
        }

        println(processBuilder.command().mkString(" "))
        val process = processBuilder.start()
        inheritIO(process.getInputStream(), System.out)
        inheritIO(process.getErrorStream(), System.err)

        process.waitFor();
      }
    }) {
      icon = new ImageIcon(Utils.readClasspathImage(
        "crystal_project/16x16/actions/player_play.png"))
    }

    contents += new Button(Action("Export...") {
      if (sm.askSaveUnchanged(this)) {
        val jarFile: File = {
          val envVarValue = System.getenv("RPGBOSS_EXPORT_JAR_PATH")
          if (envVarValue != null) {
            new File(envVarValue)
          } else {
            val classLoc =
              getClass.getProtectionDomain().getCodeSource().getLocation()
            new File(classLoc.getPath())
          }
        }

        if (jarFile.isFile()) {
          Export.export(sm.getProj, jarFile)
          val exportedDir = new File(sm.getProj.dir, "export")
          Dialog.showMessage(
              this,
              "Export complete. Packages in: \n"+
              exportedDir.getCanonicalPath(),
              "Export Complete",
              Dialog.Message.Info)
          if (Desktop.isDesktopSupported())
            Desktop.getDesktop().browse(exportedDir.toURI)
        } else {
          Dialog.showMessage(
              this,
              "Cannot locate rpgboss JAR for export. Path tried: \n" +
              jarFile.getCanonicalPath() + "\n\n" +
              "If you are running from an IDE or SBT for development,\n" +
              "set the RPGBOSS_EXPORT_JAR_PATH environment variable.",
              "Cannot export",
              Dialog.Message.Error)
        }
      }
    }) {
      icon = new ImageIcon(Utils.readClasspathImage(
        "crystal_project/16x16/actions/fileexport.png"))
    }
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
  for (map <- initialMap;
       node <- mapSelector.getNode(map.name)) {
    mapSelector.highlightWithoutEvent(node)
  }

  mainP.revalidate()
}

