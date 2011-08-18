package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._

import java.awt.Color

import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

import rpgboss.model._
import rpgboss.message._

/*
class TilesetNewPanel(val mainP: MainPanel)
extends GridBagPanel with HttpPanel
{
  preferredSize = new Dimension(800, 600)
  
  val defInsets = new Insets(5,5,5,5)
  
  add(new Label("New tileset"), new Constraints {
    gridx = 0
    gridy = 0
    gridwidth = 2
    insets = defInsets
  })
  
  def c(x: Int, y: Int) = new Constraints {
    gridx = x
    gridy = y
    insets = defInsets
    anchor = GridBagPanel.Anchor.West
  }
  
  def rightLabel(s: String) = new Label {
    text = s
  }
  
  val nameField = new TextField(mainP.objName.rName) {
    enabled = false
    columns = 12
  }
  add(rightLabel("Name:"), c(0, 1))
  add(nameField, c(1,1))
  
  val tilesizeField = new ComboBox(List(16, 32))
  add(rightLabel("Tile size (px):"), c(0, 2))
  add(tilesizeField, c(1, 2))
  
  val xTilesField_j = new JSpinner(new SpinnerNumberModel(30, 6, 60, 1))
  val yTilesField_j = new JSpinner(new SpinnerNumberModel(16, 6, 60, 1))
  
  add(rightLabel("X Tiles:"), c(0,3))
  add(Component.wrap(xTilesField_j), c(1,3))
  
  add(rightLabel("Y Tiles:"), c(0,4))
  add(Component.wrap(yTilesField_j), c(1,4))
  
  val submitBtn = new Button {
    text = "Create"
  } 
  
  add(submitBtn, c(1, 5))
  
  listenTo(submitBtn)
  
  reactions += {
    case ButtonClicked(submitBtn) => {
      submitBtn.enabled_=(false)
      
      val metadata = TilesetMetadata(tilesizeField.selection.item,
                                     xTilesField_j.getValue.asInstanceOf[Int],
                                     yTilesField_j.getValue.asInstanceOf[Int])
      
      httpSend(NewTileset(mainP.head, metadata), _ match {
        case x => mainP.error(x.toString)
      })
    }
  }
  
  
}*/

