package rpgboss.editor.resourceselector

import rpgboss.editor.StateMaster
import scala.swing._
import rpgboss.model.resource._
import rpgboss.model._

class SoundField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit)
  extends BrowseField[SoundSpec](owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
      owner, sm, Some(fieldName.text),
      true, Sound,
      newOpt => fieldName.text = newOpt.getOrElse(""))
    diag.open()
  }
}