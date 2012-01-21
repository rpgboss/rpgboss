package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._

case class MsgfontMetadata()

case class Msgfont(proj: Project, name: String, 
                   metadata: MsgfontMetadata)
extends Resource[Msgfont, MsgfontMetadata]
{
  def meta = Msgfont
  
}

object Msgfont extends MetaResource[Msgfont, MsgfontMetadata] {
  def rcType = "msgfont"
  def keyExt = "ttf"
  
  def defaultInstance(proj: Project, name: String) = 
    Msgfont(proj, name, MsgfontMetadata())
}
