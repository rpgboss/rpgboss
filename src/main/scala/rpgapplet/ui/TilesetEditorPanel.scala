package rpgboss.rpgapplet.ui

import scala.swing._
import event._

class TilesetEditorPanel extends BoxPanel(Orientation.Horizontal) {
  val button = new Button("Press here!")
  
  contents ++ List(button)
}

