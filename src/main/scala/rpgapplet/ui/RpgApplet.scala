package rpgboss.rpgapplet.ui

import scala.swing._
import scala.swing.event._

class RpgApplet extends Applet {
  object ui extends UI with Reactor {
    def init() = {
      val username = getParameter("username")
      val token = getParameter("token").toLong
      val toEdit = getParameter("toEdit")
      contents = new MainPanel(username, token, toEdit)
    }
  }
}

// vim: set ts=4 sw=4 et:
