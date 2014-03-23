package rpgboss.lib

object StringUtils {
  /**
   * Standard "42: Name" format.
   */
  def standardIdxFormat(idx: Int, name: String) = "%d: %s".format(idx, name)
}