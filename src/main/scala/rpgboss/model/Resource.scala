package rpgboss.model

import rpgboss.lib._

object Resource {
  val resourceTypes = Map("tileset"->Tileset)
  
  def listShared = listForDir(
    List(Paths.profiles, owner, Paths.resources))
  
  def listforGame(game: String) = listForDir(
    List(Paths.profiles, owner, Paths.games, game, Paths.resources))
  
  def listForDir(path: List[String]) = {
    
  }
}

trait Resource {
  def resourceType: String
  
  def owner: String
  def game: String
  def name: String
  
  def path = {
    val pathList = List(Paths.profiles, owner) ++
      (if(game.isEmpty) Nil else List(Paths.games, game)) ++ 
      List(Paths.resources, resourceType, name)
    
    pathList.mkString("/")
  }
}
