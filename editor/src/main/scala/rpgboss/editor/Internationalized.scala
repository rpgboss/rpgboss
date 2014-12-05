package rpgboss.editor

import java.util.Locale
import java.util.ResourceBundle
import com.typesafe.scalalogging.slf4j.LazyLogging

object Internationalized extends LazyLogging {
  private val currentLocale = Locale.getDefault()
  logger.debug("Current locale: " + currentLocale.toString())

  private val messages =
    ResourceBundle.getBundle("editorMessages", Internationalized.currentLocale)

  def getMessage(key: String) = messages.getString(key)
}