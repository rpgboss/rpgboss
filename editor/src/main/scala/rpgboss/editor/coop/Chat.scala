package rpgboss.editor.coop

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
import rpgboss.editor.Internationalized._ 


class Chat extends MainFrame {
  val la = new Label(getMessage("RPGBoss_Global_Chat"))

  la.foreground = Color.BLUE
  title = "RPGBoss Global Chat"

  minimumSize = new Dimension(800, 300)
  centerOnScreen()
  open()

  val myList = new ListBuffer[String]()
  val listView = new ListView[String](myList)

  val textfield:TextField = new TextField("")

  var messageLatch: CountDownLatch = _

  var SocketSession:Session = null

  new Thread(new Runnable() {
    override def run() {
      try {
        messageLatch = new CountDownLatch(1)
        val cec = ClientEndpointConfig.Builder.create().build()
        val client = ClientManager.createClient()
        client.connectToServer(new Endpoint() {

          override def onOpen(session: Session, config: EndpointConfig) {
            try {
              SocketSession = session
              session.addMessageHandler(new MessageHandler.Whole[String]() {

                override def onMessage(message: String) {
                  println("Received message: " + message)
                  myList += message
                  listView.listData = myList
                  messageLatch.countDown()
                }
              })
              //session.getBasicRemote.sendText("me<>get-users")
            } catch {
              case e: IOException => e.printStackTrace()
            }
          }
        //}, cec, new URI("ws://localhost:8080/"))
        }, cec, new URI("ws://rpgboss.hendrikweiler.com:8080/"))
        messageLatch.await(100, TimeUnit.SECONDS)
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }).start();



  contents = new BoxPanel(Orientation.Vertical) {
    contents += la
    contents += Swing.VStrut(10)
    contents += Swing.Glue
    contents += new ScrollPane(listView)
    contents += Swing.VStrut(1)
    contents += textfield
    contents += Swing.VStrut(1)
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Button("Send") { sendText() }
      contents += Swing.HStrut(5)
      contents += Button("Hide") { hideMe() }
      contents += Swing.HStrut(5)
      contents += Button("How much users") { showUsers() }
    }
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  def sendText() = {
    if(SocketSession!=null) {
      SocketSession.getBasicRemote.sendText("msg<>"+textfield.text)
      myList += textfield.text
      listView.listData = myList
      textfield.text = ""
    }
  }

  def showUsers() = {
    if(SocketSession!=null) {
      SocketSession.getBasicRemote.sendText("me<>get-users")
    }
  }

  def hideMe() = {
    visible = false
  }

  def show = {
    visible = true
  }

}