package rpgboss.model

import java.io.File

// Encapsulates behavior of URL=>name=>path, finding objects

case class ObjName(owner: String, game: Option[String], 
                   resourceTuple: Option[(String, String)])
{
  import ObjPath._
  
  def rType = resourceTuple.get._1
  def rName = resourceTuple.get._2
  
  def defined = !owner.isEmpty && (game.isDefined || resourceTuple.isDefined)
  
  def partialPath = if(!defined) "" else {
    var rcPortion =
      if(resourceTuple.isDefined) List(ObjPath.rcsDir, rType, rName) else Nil
    
    List(List(owner), gamePath(game), rcPortion).flatten.mkString("/")
  }
  
  def dirFile = new File(fileRoot + "/" + partialPath)
  def exists = dirFile.isDirectory
}

object ObjName {
  def resolve(partialPath: String) = 
    partialPath.split("/").filterNot(_.isEmpty) match {
      case Array(username, ObjPath.rcsDir, rType, rName) =>
        ObjName(username, None, Some(rType->rName))
      case Array(username, ObjPath.gamesDir, gName, 
                ObjPath.rcsDir, rType, rName) =>
        ObjName(username, Some(gName), Some(rType->rName))
      case Array(username, ObjPath.gamesDir, gName) =>
        ObjName(username, Some(gName), None)
      case _ =>
        ObjName("", None, None)
    }
}

object ObjPath {
  val fileRoot  = "/var/rpgboss"
  
  val rcsDir   = "rcs"
  val gamesDir = "g"
  
  def profilePath(username: String) = List(fileRoot, username)
  
  def gamePath(game: Option[String]) = game match {
    case Some(gName) => List(ObjPath.gamesDir, gName)
    case _ => Nil
  }
  
  // returns path to resources directory for an owner and possibly game
  def rcPath(owner: String, game: Option[String]) =
    List(profilePath(owner), gamePath(game), List(rcsDir)).flatten.mkString("/")
}
