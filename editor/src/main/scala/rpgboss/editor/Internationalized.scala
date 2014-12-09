package rpgboss.editor

import java.util.Locale
import java.util.ResourceBundle
import com.typesafe.scalalogging.slf4j.LazyLogging

object Internationalized extends LazyLogging {
  private val currentLocale = Locale.getDefault()
  logger.debug("Current locale: " + currentLocale.toString())

  private val messages =
    ResourceBundle.getBundle("editorMessages", Internationalized.currentLocale)

  /**
   * Convert to allow .properties file to be in UTF-8.
   * Technique from: http://stackoverflow.com/a/6995374.
   */
  def getMessage(key: String) = {
    val rawValue = messages.getString(key)
    new String(rawValue.getBytes("ISO-8859-1"), "UTF-8")
  }

  def getMessageColon(key: String) = getMessage(key) + ":"
}