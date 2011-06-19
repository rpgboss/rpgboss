package rpgboss.model

import rpgboss.lib._

import java.io.File
import FileHelper._
import scala.collection.JavaConversions._

object Resource {
  val resourceTypes = Map("tileset"->Tileset)
  
  def listShared(owner: String) = listForDir(
    List(Paths.profiles, owner, Paths.resources))
  
  def listforGame(owner: String, game: String) = listForDir(
    List(Paths.profiles, owner, Paths.games, game, Paths.resources))
  
  def listForDir(path: List[String]) = {
    val rcDir = new File(path.mkString("/"))
    
    def walkResourceDir(rcType: String) : List[String] = {
      val rcTypeDir = new File(rcDir, rcType)
      
      if(rcTypeDir.forceMkdirs()) 
        Nil 
      else // subdirs
        rcTypeDir.listFiles.toList.filter(f => f.isDirectory).map(_.toString) 
    }
    
    if(rcDir.forceMkdirs())
      resourceTypes.mapValues( v => Nil )
    else {
      resourceTypes.map( kv => walkResourceDir(kv._1) )
    }
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
