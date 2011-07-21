package rpgboss.message

import rpgboss.model._
import net.liftweb.json._

case class Header(username: String, token: Long, name: ObjName)

abstract class Message

abstract class RequestMessage extends Message {
  def head: Header 
}

abstract class ResponseMessage extends Message

case class RequestItem(head: Header) 
extends RequestMessage

case class NewTileset(head: Header, metadata: TilesetMetadata)
extends RequestMessage

case class NoSuchItem() extends ResponseMessage
case class AuthFailure() extends ResponseMessage

object Message {
  
  val formats  = Serialization.formats(FullTypeHints(List(
    classOf[RequestItem],
    classOf[NewTileset],
    classOf[NoSuchItem],
    classOf[AuthFailure]
  )))
}




// vim: set ts=4 sw=4 et:
