package rpgboss.editor

import java.util.Locale
import java.util.ResourceBundle
import com.typesafe.scalalogging.slf4j.LazyLogging

trait Internationalized extends LazyLogging {
  val messages =
    ResourceBundle.getBundle("editorMessages", Internationalized.currentLocale)
}

object Internationalized extends LazyLogging {
  val currentLocale = Locale.getDefault()
  logger.debug("Current locale: " + currentLocale.toString())
}