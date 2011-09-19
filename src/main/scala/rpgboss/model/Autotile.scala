package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._

case class Autotile(p: Project,
                    name: String)
extends HasName
{
}
