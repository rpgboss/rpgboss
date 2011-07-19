package rpgboss.rpgapplet.ui

import scala.swing._
import event._

import rpgboss.model._
import rpgboss.message._

import net.liftweb.json._

import org.apache.http.impl.client._
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPost

class MainPanel(val username: String, val token: Long, val toEdit: String)
extends BoxPanel(Orientation.Vertical) 
{
  val objName = ObjName.resolve(toEdit)
  
  contents += new Label("Loading resource: " + toEdit)
  
   
  val http = new DefaultHttpClient()
  
  try {
    implicit val formats = Message.formats
    
    val jsonReq = Serialization.write(RequestItem(username, token, objName))
  
    val post = new HttpPost("http://devpersonal:8080/editorApplet/cmd")
    post.setEntity(new StringEntity(jsonReq))
    post.addHeader("Content-Type", "application/json")
  
    val handler = new BasicResponseHandler() 
  
    val respStr = http.execute(post, new BasicResponseHandler())
    
    val respMsg = Serialization.read[ResponseMessage](respStr)
      
    val resp = respMsg match {
      case NoSuchItem() => "No such item!"
      case _ => respStr
    }
    
    contents += new Label(resp)
  
  } finally {
    http.getConnectionManager.shutdown()
  }
}

