package rpgboss.rpgapplet.lib

import scala.swing._

import org.apache.http._
import org.apache.http.client._
import org.apache.http.impl.client._
import org.apache.http.util._
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.client.methods.HttpPost

trait HttpSender
{
  def spawn(p: => Unit) : Unit = {
    val t = new Thread() { override def run() = p }
    t.start()
  }
  
  def httpSend[T](cmdStr: String,
                  request: { def toByteArray() : Array[Byte] }, 
                  parserFunc: Array[Byte] => T,
                  respFunc: T => Any,
                  failFunc: () => Any) = 
  spawn {
    val http = new DefaultHttpClient()
    
    try {
      val post = new HttpPost("http://devpersonal:8080/editorApi/" + cmdStr)
      post.setEntity(new ByteArrayEntity(request.toByteArray()))
      post.addHeader("Content-Type", "application/octet-stream")
    
      val handler = new ResponseHandler[Array[Byte]] {
        def handleResponse(response: HttpResponse) : Array[Byte] = {
          val entity = response.getEntity()
          
          if(entity != null) EntityUtils.toByteArray(entity) else Array.empty
        }
      }
    
      val respBytes = http.execute(post, handler)
      
      val respObj = parserFunc(respBytes)
      
      Swing.onEDT({respFunc(respObj)})
    
    } catch {
      case _ => failFunc()
    } finally {
      http.getConnectionManager.shutdown()
    }
  }
  
  
}
