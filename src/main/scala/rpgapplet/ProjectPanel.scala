package rpgboss.rpgapplet

import rpgboss.rpgapplet.dialog._
import rpgboss.rpgapplet.lib._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class ProjectPanel(val mainP: MainPanel, val project: Project)
  extends BoxPanel(Orientation.Horizontal)
{
  /*val tileSelector = new BoxPanel(Orientation.Vertical)
  val mapSelector = new MapSelector(project, tileSelector)
  
  contents += new SplitPane(Orientation.Vertical, tileSelector, mapSelector)
  contents += new MapView()*/
}

