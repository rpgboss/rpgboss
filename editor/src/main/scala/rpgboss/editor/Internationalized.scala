package rpgboss.editor

import java.util.Locale
import java.util.ResourceBundle
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.util.MissingResourceException

object Internationalized extends LazyLogging {
  private val currentLocale = Locale.getDefault()
  logger.debug("Current locale: " + currentLocale.toString())

  private val messages =
    ResourceBundle.getBundle("editorMessages", Internationalized.currentLocale)

  /**
   * Convert to allow .properties file to be in UTF-8.
   * Technique from: http://stackoverflow.com/a/6995374.
   */
  def getMessage(key: String): String = {
    try {
      val rawValue = messages.getString(key)
      return new String(rawValue.getBytes("ISO-8859-1"), "UTF-8")
    } catch {
      case e: MissingResourceException => {
        logger.error(e.getMessage())
        return "$MISSINGTRANSLATIONKEY: %s".format(key)
      }
    }
  }

  def getMessageColon(key: String) = getMessage(key) + ":"

  def needsTranslation(key: String) = {
    logger.warn("Accessed key \"%s\" needs translation.".format(key))
    key
  }

  def needsTranslationColon(key: String) = {
    needsTranslation(key) + ":"
  }

  def obsoleteMessage(key: String) = {
    if (messages.containsKey(key)) {
      logger.error("""Key "%s" may be no longer needed. Remove from translation
          message files, then remove obsoleteMessage("%s") call.""".format(
              key, key))
    } else {
      logger.error(
          """Call to obsoleteMessage("%s") should be removed.""".format(key))
    }
  }
}