package rpgboss.rpgapplet

import java.util.Properties

import java.io._
import rpgboss.lib.FileHelper._

object Settings {
  val props = new Properties()
  
  def propsFile = new File(
    System.getProperty("user.home") + File.separator + ".rpgboss" +
    File.separator + "editor.props")
  
  if(propsFile.isFile && propsFile.canRead)
    props.load(new FileInputStream(propsFile))
  
  def get(k: String) = Option(props.getProperty(k))
  
  def set(k: String, v: String) = {
    props.setProperty(k, v)
    propsFile.prepareWrite({ fos =>
      props.store(fos, "rpgboss editor settings")
      true
    })
  }
}
