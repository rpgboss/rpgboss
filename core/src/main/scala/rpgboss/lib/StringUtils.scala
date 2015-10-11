package rpgboss.lib

import rpgboss.model.HasName

object StringUtils {
  /**
   * Standard "42: Name" format.
   */
  def standardIdxFormat(idx: Int, name: String) = "%d: %s".format(idx, name)
}

class DistinctCharacterSet extends collection.mutable.HashSet[Char] {
  def add(x: HasName): Unit =
    add(x.name)

  def add(x: String): Unit =
    this ++= x.iterator

  def addAll[T <: HasName](xs: Array[T]) = xs.foreach(add)

  def addAll(xs: Array[String]) = xs.foreach(add)
}