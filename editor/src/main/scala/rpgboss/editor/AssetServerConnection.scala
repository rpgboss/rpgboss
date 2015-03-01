package rpgboss.editor

import java.awt.Color
import scala.swing._
import scala.swing.event._

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

import rpgboss.lib.Utils
import java.awt.Cursor

import rpgboss.editor.dialog.SettingsDialog

class ConnectionData(
	var action:String = "",
	var value:String = "",
	var value2:String = "",
	var value3:String = "") {

}

object VisibleConnection {

	var label = new Button("")
	label.borderPainted = false
	label.cursor = new Cursor(Cursor.HAND_CURSOR)

	var panel = new BoxPanel(Orientation.Horizontal) {

		update("Checking...")

		listenTo(label)

		reactions += {
			case ButtonClicked(label) =>
	    	settingsDialog.open()
		}

		contents += label

	}

	var authenticated:Boolean = false

	var connection:AssetServerConnection = null
	var settingsDialog:SettingsDialog = null
	var currentSession:Session = null

	def update(newValue:String) {
		label.text = "<html><font color=\"#0000CF\"><u>"+newValue+"</u></font></html>";
	}
}

class AssetServerConnection(val mainP: MainPanel,sm: StateMaster) {

	implicit lazy val formats = DefaultFormats

	var testConnection = Http("http://google.de").timeout(connTimeoutMs = 1000, readTimeoutMs = 1500).asString
	var noConnection = false
	var unreachable = false

	var connectionThread:Thread = null

  def ready() = {
    val host = Settings.get("assetserver.host")
    val username = Settings.get("assetserver.username")
    val password = Settings.get("assetserver.password")
    host.isDefined && username.isDefined && password.isDefined
  }

	def restart():Unit = {
		if(connectionThread!=null) {
			connectionThread.interrupt()
		}
		start()
	}

	def start():Unit = {
    var host = Settings.get("assetserver.host").get
    var username = Settings.get("assetserver.username").get
    var password = Settings.get("assetserver.password").get

		var currentSessionString = ""

		if(username!="" && password!="") {
			try {
				var response = Http(Settings.get("assetserver.host").get+"/api/v1/login/"+username+"/"+password).asString
				currentSessionString = response.body
			}
			catch {
				case e:Exception =>
					unreachable = true
					println("no asset server?")
			}
		}

		if(host=="") {
			host = "assets.rpgboss.com"
		} else {
			host = host.replace("http://","")
			host = host.replace("https://","")
		}
		if(username=="" || password=="") {
			noConnection = true
		}

		if(currentSessionString=="") {
				VisibleConnection.update("Authentication error")
				VisibleConnection.authenticated = false
			} else {
				VisibleConnection.update("Authenticated")
				VisibleConnection.authenticated = true

				connectionThread = new Thread(new Runnable() {
				  override def run() {
				    try {
				    	val projPath = sm.getProj.dir.getCanonicalPath()
				      var messageLatch = new CountDownLatch(1)
				      val cec = ClientEndpointConfig.Builder.create().build()
				      val client = ClientManager.createClient()
				      try {
					      client.connectToServer(new Endpoint() {
					      	override def onClose(session:Session, reason:CloseReason) {
					      		VisibleConnection.update("Disconnected.")
					      		//if(!unreachable)	restart()
					      	}
					      	override def onError(session:Session, t:Throwable) {
					      		VisibleConnection.update("Disconnected.")
					      		//if(!unreachable)	restart()
					      	}
					        override def onOpen(session: Session, config: EndpointConfig) {
					        	VisibleConnection.update("Connected to the asset server")

					        	VisibleConnection.currentSession = session

					        	unreachable = false

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
						              		var id = data.value
						              		var name = data.value2
						              		// TODO: check if package is already in the project
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
					      }, cec, new URI("ws://"+host+":8080/"))
								messageLatch.await(100, TimeUnit.SECONDS)
							}
							catch {
					      case e: Exception => e.printStackTrace()
							}
				    } catch {
				      case e: Exception => e.printStackTrace()
				    }
				  }
				});
				connectionThread.start()


		}
	}

}