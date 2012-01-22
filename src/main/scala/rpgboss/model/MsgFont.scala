package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.Utils._
import rpgboss.lib.FileHelper._

import java.awt._
import java.io._

case class MsgfontMetadata()

case class Msgfont(proj: Project, name: String, 
                   metadata: MsgfontMetadata)
extends Resource[Msgfont, MsgfontMetadata]
{
  def meta = Msgfont
  
  val font = Font.createFont(Font.TRUETYPE_FONT, dataFile)
    .deriveFont(proj.data.fontsize)
}

object Msgfont extends MetaResource[Msgfont, MsgfontMetadata] {
  def rcType = "msgfont"
  def keyExt = "ttf"
  
  def defaultInstance(proj: Project, name: String) = 
    Msgfont(proj, name, MsgfontMetadata())
}
