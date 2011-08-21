package rpgboss.rpgapplet.dialog

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
  extends StdDialog(owner, "Login")
{
  val usernameField = new TextField() {
    columns = 12
  }
  val passwordField = new PasswordField() {
    columns = 12
  }
  
  def okFunc() = {
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
        statusLabel.text = "Wrong username/password."
        loginFunc(None)
        passwordField.requestFocus()
      }
    }, () => {
      // failure function
      statusLabel.text = "Network failure."
    })
  }
  
  val statusLabel = new Label(" ") 
  
  contents = new DesignGridPanel {
    row().center().add(new Label("rpgboss"))
    
    row().grid(new Label("Username:")).add(usernameField)
    row().grid(new Label("Password:")).add(passwordField)
    
    row().center().fill().add(statusLabel)
    
    addButtons(cancelButton, okButton)
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

