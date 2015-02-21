package rpgboss.editor.dialog

import rpgboss.editor.uibase._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.misc.Paths
import scala.swing._
import scala.swing.event._
import rpgboss.model._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import java.io.File
import rpgboss.editor.Settings
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.Internationalized._
import javax.swing.BorderFactory


class SettingsDialog(owner: Window, onSuccess: Project => Any)
  extends StdDialog(owner, getMessage("Settings")) {

  centerDialog(new Dimension(400, 400))

  def okFunc() = {
    close()
  }

  if(Settings.get("assetserver.host")=="") {
    Settings.set("assetserver.host", "http://assets.rpgboss.com")
  }

  var assetserver_host = textField(Settings.get("assetserver.host").get, Settings.set("assetserver.host", _))
  var assetserver_username = textField(Settings.get("assetserver.username").get, Settings.set("assetserver.username", _))
  var assetserver_password = textField(Settings.get("assetserver.password").get, Settings.set("assetserver.password", _))

  contents = new DesignGridPanel {
    border = BorderFactory.createTitledBorder(getMessage("Asset_Server"))
    row().grid().add(leftLabel(getMessageColon("Asset_Server_Hostname")))
    row().grid().add(assetserver_host)
    row().grid().add(leftLabel(getMessageColon("Username")))
    row().grid().add(assetserver_username)
    row().grid().add(leftLabel(getMessageColon("Password")))
    row().grid().add(assetserver_password)

    addButtons(okBtn, cancelBtn)
  }

  //listenTo(projList.mouse.clicks)

  reactions += {
    case MouseClicked(`okBtn`, _, _, 2, _) => okBtn.doClick()
  }
}