package rpgboss.editor

import scala.swing._

import rpgboss.lib._
import rpgboss.model._
import rpgboss.message._
import rpgboss.editor.tileset._

class MapSelector(sm: StateMaster, projPanel: ProjectPanel) 
extends Tree[HasName] 
{
  
  preferredSize = new Dimension(8*32, 200)
  
  def mapMetas = sm.getMapMetas
  
  treeData = {
    // need to make a value copy of sm.proj 
    // because can only match on stable values
    val proj = sm.proj
    
    TreeModel[HasName](proj)({
      case `proj` => mapMetas.filter(_.parent == -1)
      case m: RpgMap => mapMetas.filter(_.parent == m.id)
    })
  }

  expandPath(sm.proj :: Nil)
}
