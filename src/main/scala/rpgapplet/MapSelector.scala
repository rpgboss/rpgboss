package rpgboss.rpgapplet

import scala.swing._

import rpgboss.lib._
import rpgboss.model._
import rpgboss.message._
import rpgboss.rpgapplet.tileset._

class MapSelector(project: Project, tsSidebar: TilesetSidebar) 
extends Tree[HasName] 
{
  var maps = project.getMaps
  
  treeData = TreeModel[HasName](project)({
    case `project` => maps.filter(_.parent == -1)
    case m: RpgMap => maps.filter(_.parent == m.id)
  })

  expandPath(project :: Nil)
  
  //tsSidebar.select
}
