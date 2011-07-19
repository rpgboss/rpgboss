package rpgboss.message

import rpgboss.model._
import net.liftweb.json._

trait Message

trait RequestMessage extends Message {
  def username: String
  def token: Long
}

trait ResponseMessage extends Message

case class RequestItem(username: String, token: Long, name: ObjName) 
extends RequestMessage

case class NoSuchItem() extends ResponseMessage
case class AuthFailure() extends ResponseMessage

object Message {
  
  val formats  = Serialization.formats(FullTypeHints(List(
    classOf[RequestItem],
    classOf[NoSuchItem],
    classOf[AuthFailure]
  )))
}




// vim: set ts=4 sw=4 et:
