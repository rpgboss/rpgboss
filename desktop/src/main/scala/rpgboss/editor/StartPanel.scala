package rpgboss.editor

import rpgboss.lib._

import rpgboss.editor.dialog._
import rpgboss.editor.uibase._

import scala.swing._
import scala.swing.event._

import javax.imageio._
import javax.swing.ImageIcon

import rpgboss.model._
import rpgboss.model.resource._

class StartPanel(val mainP: MainPanel)
  extends BoxPanel(Orientation.Vertical) {

  val img = new Label { 
  	icon = new ImageIcon(rpgboss.lib.Utils.readClasspathImage("hendrik-weiler-theme/splash.jpg")) 
  }
  contents += new BorderPanel {
  	add(img, BorderPanel.Position.East)
  }

  contents += new BoxPanel(Orientation.Horizontal) {
	contents += new Button(mainP.actionNew)
    contents += Swing.HStrut(32)
	contents += new Button(mainP.actionOpen)
  }
}

