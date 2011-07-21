package rpgboss.rpgapplet

import scala.swing._
import event._

import rpgboss.model._
import rpgboss.message._

class LoadingPanel(val mainP: MainPanel)
extends BoxPanel(Orientation.Vertical) with HttpPanel
{
  contents += new Label("Loading resource: " + mainP.toEdit)
  
  httpSend(RequestItem(mainP.head), _ match {    
    case NoSuchItem() =>
      mainP.handleNoSuchItem()
    case x => 
      mainP.error(x.toString)
  })
}

