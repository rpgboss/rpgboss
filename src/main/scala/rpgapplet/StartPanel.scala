package rpgboss.rpgapplet

import rpgboss.rpgapplet.dialog._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class StartPanel(val mainP: MainPanel)
  extends BoxPanel(Orientation.Horizontal)
{
  preferredSize = new Dimension(800, 600)
  
  contents += new Label("Logo here")
  
  contents += new BoxPanel(Orientation.Vertical) {
    contents += new Button(Action("New Game Project") {
      val d = new NewProjectDialog(mainP.topWin, p => Unit)
      d.open()
    })
    
    contents += new Button(Action("Load Game Project") {
      LoginDialog.loginFirst(mainP.topWin) {
        println("Login success")
      }
    })
  }
}

