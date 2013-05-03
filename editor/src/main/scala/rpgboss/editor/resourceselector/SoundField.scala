package rpgboss.editor.resourceselector

import rpgboss.editor.StateMaster
import scala.swing._
import rpgboss.model.resource._
import rpgboss.model._

class SoundFieldBase(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit,
  isMusic: Boolean)
  extends BrowseField[SoundSpec](owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new SoundSelectDialog(
      owner, sm, initial, model = _, isMusic)
    diag.open()
  }
}

class SoundField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit)
  extends SoundFieldBase(owner, sm, initial, onUpdate, false)

class MusicField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit)
  extends SoundFieldBase(owner, sm, initial, onUpdate, true)

class SoundSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  override val onSuccess: Option[SoundSpec] => Unit,
  isMusic: Boolean)
  extends ResourceSelectDialogBase(owner, Sound) {

  override val resourceSelector = new ResourceSelectPanel(
    sm,
    initial,
    true,
    Sound) {
    
    override def specToResourceName(spec: SoundSpec): String = spec.sound

    override def newRcNameToSpec(name: String,
                                 prevSpec: Option[SoundSpec]): SoundSpec =
      prevSpec.getOrElse(SoundSpec("")).copy(sound = name)
  }
}