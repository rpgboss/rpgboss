package rpgboss.rpgapplet

import rpgboss.rpgapplet.dialog._
import rpgboss.rpgapplet.lib._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class StartPanel(val mainP: MainPanel)
  extends BoxPanel(Orientation.Horizontal)
{ 
  contents += new Label("Logo here")
  
  contents += new BoxPanel(Orientation.Vertical) {
    contents += new Button(Action("New Game Project") {
      val d = new NewProjectDialog(mainP.topWin, p => mainP.projectGui(p))
      d.open()
    })
    
    contents += new Button(Action("Load Game Project") {
      LoginDialog.loginFirst(mainP.topWin) {
        println("Login success")
      }
    })
  }
}

