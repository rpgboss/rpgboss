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

object SettingsOptions {
  var assetserver_host = "assets.rpgboss.com"
  var assetserver_username = ""
  var assetserver_password = ""
}


class SettingsDialog(owner: Window, onSuccess: Project => Any)
  extends StdDialog(owner, getMessage("Settings")) {

  centerDialog(new Dimension(400, 400))

  def okFunc() = {
    Settings.set("assetserver.host", SettingsOptions.assetserver_host)
    Settings.set("assetserver.username", SettingsOptions.assetserver_username)
    Settings.set("assetserver.password", SettingsOptions.assetserver_password)
    close()
  }

  var ahostValue = Settings.get("assetserver.host")
  var ausernameValue = Settings.get("assetserver.username")
  var apasswordValue = Settings.get("assetserver.password")

  if(ahostValue=="") {
    SettingsOptions.assetserver_host = ahostValue.get
  }

  var assetserver_host = textField(SettingsOptions.assetserver_host, SettingsOptions.assetserver_host = _)
  var assetserver_username = textField(SettingsOptions.assetserver_username, SettingsOptions.assetserver_username = _)
  var assetserver_password = textField(SettingsOptions.assetserver_password, SettingsOptions.assetserver_password = _)

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