package rpgboss.editor

import scala.swing._
import scala.swing.tree._

import rpgboss.lib._
import rpgboss.model._
import rpgboss.editor.tileset._

class MapSelector(sm: StateMaster, projPanel: ProjectPanel) 
extends BoxPanel(Orientation.Vertical) 
{
  /*
  preferredSize = new Dimension(8*32, 200)
  
  def mapMetas = sm.getMapMetas
  
  treeData = {
    // need to make a value copy of sm.proj 
    // because can only match on stable values
    // FIXME: not currently doing this... wtf is going on here
    val proj = sm.getProj
    
    TreeModel[Any](proj)({
      case `proj` => mapMetas.filter(_.metadata.parent < 0)
      case m: RpgMap => mapMetas.filter(_.metadata.parent == m.id)
    })
  }

  expandPath(sm.getProj :: Nil)*/
}
