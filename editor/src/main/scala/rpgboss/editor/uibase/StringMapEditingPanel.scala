package rpgboss.editor.uibase

import scala.collection.mutable.ArrayBuffer
import scala.swing.Window

import rpgboss.editor.Internationalized.needsTranslation

class StringMapEditingPanel(
  owner: Window,
  override val title: String,
  initial: Map[String, String],
  onUpdateF: Map[String, String] => Unit)
  extends TableEditor[(String, String)] {

  val modelArray = ArrayBuffer(initial.toSeq.sorted: _*)
  var modelMap = Map(modelArray: _*)
  def newInstance(): (String, String) = ("", "")

  def onUpdate() = {
    modelMap = Map(modelArray: _*)
    onUpdateF(modelMap)
  }

  def colHeaders = Array("Key", "Message")

  def getRowStrings(row: (String, String)) = Array(row._1, row._2)

  def showEditDialog(initial: (String, String),
                     okCallback: ((String, String)) => Unit) = {
    val d = new KeyValueEditingDialog(owner, modelMap, initial, okCallback)
    d.open()
  }
}
