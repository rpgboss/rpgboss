package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._

import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

import rpgboss.model._
import rpgboss.message._

import net.liftweb.json._

import org.apache.http.impl.client._
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPost

class TilesetNewPanel(val mainP: MainPanel)
extends GridBagPanel with HttpPanel
{
  add(new Label("New tileset"), new Constraints {
    gridx = 0
    gridy = 0
    gridwidth = 2
  })
  
  val nameField = new TextField(mainP.objName.rName) {
    enabled = false
  }
  add(new Label("Name:"), (0, 1))
  add(nameField, (1,1))
  
  val tilesizeField = new ComboBox(List(16, 32))
  add(new Label("Tile size (px):"), (0, 2))
  add(tilesizeField, (1, 2))
  
  val xTilesField_j = new JSpinner(new SpinnerNumberModel(30, 6, 60, 1))
  val yTilesField_j = new JSpinner(new SpinnerNumberModel(16, 6, 60, 1))
  
  add(new Label("X Tiles:"), (0,3))
  add(Component.wrap(xTilesField_j), (1,3))
  
  add(new Label("Y Tiles:"), (0,4))
  add(Component.wrap(yTilesField_j), (1,4))
  
  val submitBtn = new Button {
    text = "Create"
  } 
  
  add(submitBtn, (1, 5))
  
  listenTo(submitBtn)
  
  reactions += {
    case ButtonClicked(submitBtn) => {
      val metadata = TilesetMetadata(tilesizeField.selection.item,
                                     xTilesField_j.getValue.asInstanceOf[Int],
                                     yTilesField_j.getValue.asInstanceOf[Int])
      
      httpSend(NewTileset(mainP.head, metadata), _ match {
        case x => mainP.error(x.toString)
      })
    }
  }
  
  
}

