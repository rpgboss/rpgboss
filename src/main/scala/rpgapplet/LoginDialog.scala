package rpgboss.rpgapplet

import rpgboss.rpgapplet.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.message.Messages._

object LoginDialog {
  var auth: Option[(String, Long)] = None
  
  def loginFirst(topWin: Window)(afterLogin : => Unit) = {
    if(auth.isDefined) {
      afterLogin
    } else {
      val login = new LoginDialog(topWin, authOpt => {
        auth = authOpt
        if(auth.isDefined) afterLogin
      })
      login.open()
    }
  }
}

class LoginDialog(owner: Window, loginFunc: Option[(String, Long)] => Any) 
  extends Dialog(owner) with HttpSender
{
  modal = true
  title = "Login"
  setLocationRelativeTo(owner)
  
  val usernameField = new TextField() {
    columns = 12
  }
  val passwordField = new PasswordField() {
    columns = 12
  }
  val okButton = new Button(new Action("OK") {
    mnemonic = Key.O.id
    def apply() = {
      statusLabel.text = "Waiting..."
      
      val loginMsg =
        Login.newBuilder()
          .setUsername(usernameField.text)
          .setPassword(new String(passwordField.password))
        .build()
      
      httpSend("login", loginMsg, LoginResp.parseFrom, (r: LoginResp) => {
        if(r.getSuccess()) {
          loginFunc(Some(r.getUsername()->r.getToken()))
          close()
        } else {
          statusLabel.text = "Fail. Try again."
          loginFunc(None)
          passwordField.requestFocus()
        }
      })
    }
  })
  val statusLabel = new Label(" ") {
    xLayoutAlignment = java.awt.Component.CENTER_ALIGNMENT
  }
  
  defaultButton = okButton
  
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new Label("rpgboss") {
      xLayoutAlignment = java.awt.Component.CENTER_ALIGNMENT
    }
    
    contents += new GridBagPanel {    
      def c(x: Int, y: Int) = new Constraints {
        gridx = x
        gridy = y
        insets = new Insets(5,5,5,5)
      }
    
      add(new Label("Username:"), c(0,0))
      add(usernameField, c(1,0))
      
      add(new Label("Password:"), c(0,1))
      add(passwordField, c(1,1))
    }
    
    contents += statusLabel
    
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Swing.HGlue
      contents += okButton
      contents += new Button(Action("Cancel") { close() })
    }
  }
  
  listenTo(usernameField, usernameField.keys, passwordField, passwordField.keys)
  
  reactions += {
    case FocusGained(`usernameField`, _, false) => usernameField.selectAll()
    case FocusGained(`passwordField`, _, false) => passwordField.selectAll()
    case KeyReleased(`usernameField`, Key.Enter, _, _) =>   
      passwordField.requestFocus()
    case KeyReleased(`passwordField`, Key.Enter, _, _) => okButton.doClick()
  }
}

