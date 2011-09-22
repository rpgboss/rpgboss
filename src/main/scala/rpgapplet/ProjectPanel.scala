package rpgboss.rpgapplet

import rpgboss.rpgapplet.dialog._
import rpgboss.rpgapplet.lib._
import rpgboss.rpgapplet.tileset._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class ProjectPanel(mainP: MainPanel, project: Project)
  extends BoxPanel(Orientation.Horizontal)
{
  val stateMaster = new StateMaster(project)
  
  val tilesetSidebar = new TilesetSidebar(stateMaster)
  val mapSelector = new MapSelector(stateMaster, tilesetSidebar)
  
  contents += new SplitPane(Orientation.Horizontal, tilesetSidebar, mapSelector)
  contents += new MapView()
}

