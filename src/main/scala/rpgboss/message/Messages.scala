package rpgboss.message

import rpgboss.model._
import net.liftweb.json._

case class Header(username: String, token: Long, name: ObjName)

abstract class RequestMessage {
  def head: Header
}

abstract class ResponseMessage

case class GetTileset(head: Header) extends RequestMessage
case class NewTileset(head: Header, metadata: TilesetMetadata) 
  extends RequestMessage

case class AuthFail() extends ResponseMessage
case class NoSuchItem() extends ResponseMessage
  
case class TilesetResp(metadata: TilesetMetadata,
                       imageDataB64: String = "") 
extends ResponseMessage

object Message {
  
  val formats  = Serialization.formats(FullTypeHints(List(
    classOf[GetTileset],
    classOf[NewTileset],
    classOf[AuthFail],
    classOf[NoSuchItem],
    classOf[TilesetResp]
  )))
}

// vim: set ts=4 sw=4 et:
