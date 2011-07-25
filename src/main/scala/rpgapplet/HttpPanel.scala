package rpgboss.rpgapplet

import rpgboss.message.{Message, RequestMessage, ResponseMessage}

import net.liftweb.json._

import scala.swing._

import org.apache.http.impl.client._
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPost

trait HttpPanel
{
  def mainP: MainPanel
  
  def spawn(p: => Unit) : Unit = {
    val t = new Thread() { override def run() = p }
    t.start()
  }
  
  def httpSend(msg: RequestMessage, respFunc: ResponseMessage => Unit) = 
  spawn {
    val http = new DefaultHttpClient()
    
    try {
      implicit val formats = Message.formats
      
      val jsonReq = Serialization.write(msg)
    
      val post = new HttpPost("http://devpersonal:8080/editorApplet/cmd")
      post.setEntity(new StringEntity(jsonReq))
      post.addHeader("Content-Type", "application/json")
    
      val handler = new BasicResponseHandler() 
    
      val respStr = http.execute(post, new BasicResponseHandler())
      
      val parsedResp = JsonParser.parse(respStr)
      
      val respMsg = parsedResp.extract[ResponseMessage]
      
      println(respMsg.toString)
      
      Swing.onEDT({respFunc(respMsg)})
    
    } finally {
      http.getConnectionManager.shutdown()
    }
  }
  
  
}
