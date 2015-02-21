package rpgboss.editor

import java.awt.Color
import scala.swing._
import scala.collection.mutable._

import javax.websocket._
import scala.collection.JavaConversions._

import org.glassfish.tyrus._
import org.glassfish.tyrus.core._

import java.io._
import java.net._
import java.util.concurrent._
import org.glassfish.tyrus.client._
import com.typesafe.scalalogging.slf4j.LazyLogging

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.Formats

import scalaj.http.Http

class ConnectionData(
	var action:String = "",
	var value:String = "",
	var value2:String = "",
	var value3:String = "") {

}

object VisibleConnection {
	var status = "Checking..."

	var label = new Label(status)

	def update(newValue:String) {
		label.text = newValue
	}
}

class AssetServerConnection(val mainP: MainPanel,sm: StateMaster) {

	implicit lazy val formats = DefaultFormats

	var testConnection = Http("http://google.de").timeout(connTimeoutMs = 1000, readTimeoutMs = 1500).asString
	var noConnection = false

	var host = Settings.get("assetserver.host").get
	var username = Settings.get("assetserver.username").get
	var password = Settings.get("assetserver.password").get
	if(host=="") {
		host = "assets.rpgboss.com"
	} else {
		host = host.replace("http://","")
		host = host.replace("https://","")
	}
	if(username=="" || password=="") {
		noConnection = true
	}

	def start():Unit = {

		var response = Http(Settings.get("assetserver.host").get+"/api/v1/login/"+username+"/"+password).asString
		var currentSessionString = response.body

		if(currentSessionString=="") {
				VisibleConnection.update("Authentication error")
			} else {
				VisibleConnection.update("Authenticated")

			new Thread(new Runnable() {
			  override def run() {
			    try {
			    	val projPath = sm.getProj.dir.getCanonicalPath()
			      var messageLatch = new CountDownLatch(1)
			      val cec = ClientEndpointConfig.Builder.create().build()
			      val client = ClientManager.createClient()
			      client.connectToServer(new Endpoint() {
			      	override def onClose(session:Session, reason:CloseReason) {
			      		VisibleConnection.update("Disconnected.")
			      		start()
			      	}
			      	override def onError(session:Session, t:Throwable) {
			      		VisibleConnection.update("Disconnected.")
			      		start()
			      	}
			        override def onOpen(session: Session, config: EndpointConfig) {
			        	VisibleConnection.update("Connected to the asset server")

			        	session.getBasicRemote.sendText("set;editor;{\"value\":\""+currentSessionString+"\"}")
			          try {
			            //SocketSession = session
			            session.addMessageHandler(new MessageHandler.Whole[String]() {
			              override def onMessage(message: String) {
			                
			              	var split = message.split(";")
			              	var mode = split(1)
			              	var command = split(0)
			              	var json = parse(split(2))

											var data = json.extract[ConnectionData](formats,Manifest.classType(classOf[ConnectionData]))

			              	if(command=="command") {


				              	if(data.action=="getProjectData") {
				              		session.getBasicRemote.sendText("command;editor;{\"action\":\"getProjectData\",\"value\":\""+projPath+"\"}")
				              	}

				              	if(data.action=="startDownload") {
				              		println("start download" + data.value)
				              		mainP.visible = true
				              	}

			              	}

			                messageLatch.countDown()
			              }
			            })
			          } catch {
			            case e: IOException => e.printStackTrace()
			          }
			        }
			      }, cec, new URI("ws://"+host+":8081/"))
			      messageLatch.await(100, TimeUnit.SECONDS)
			    } catch {
			      case e: Exception => e.printStackTrace()
			    }
			  }
			}).start();


		}
	}

}