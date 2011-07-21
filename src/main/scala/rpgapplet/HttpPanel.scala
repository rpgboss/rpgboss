package rpgboss.rpgapplet

import rpgboss.message._

import net.liftweb.json._

import scala.swing._

import org.apache.http.impl.client._
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPost

trait HttpPanel
{
  def mainP: MainPanel
  
  def httpSend(msg: Message, respFunc: Any => Unit) = concurrent.ops.spawn({
    val http = new DefaultHttpClient()
    
    try {
      implicit val formats = Message.formats
      
      val jsonReq = Serialization.write(msg)
    
      val post = new HttpPost("http://devpersonal:8080/editorApplet/cmd")
      post.setEntity(new StringEntity(jsonReq))
      post.addHeader("Content-Type", "application/json")
    
      val handler = new BasicResponseHandler() 
    
      val respStr = http.execute(post, new BasicResponseHandler())
      
      val respMsg = Serialization.read[Any](respStr)
      
      Swing.onEDT({respFunc(respMsg)})
    
    } finally {
      http.getConnectionManager.shutdown()
    }
  })
  
  
}
