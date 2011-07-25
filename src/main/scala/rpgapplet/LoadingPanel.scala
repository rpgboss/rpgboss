package rpgboss.rpgapplet

import scala.swing._
import event._

import rpgboss.model._
import rpgboss.message._

class LoadingPanel(val mainP: MainPanel)
extends BoxPanel(Orientation.Vertical) with HttpPanel
{
  preferredSize = new Dimension(800, 600)
  
  contents += new Label("Loading resource: " + mainP.toEdit)
  
  httpSend(GetTileset(mainP.head), _ match {    
    case NoSuchItem() =>
      mainP.handleNoSuchItem()
    case x => 
      mainP.error(x.toString)
  })
}

