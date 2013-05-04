package rpgboss.editor.resourceselector

import rpgboss.editor.StateMaster
import scala.swing._
import rpgboss.model.resource._
import rpgboss.model._
import rpgboss.editor.uibase._
import com.badlogic.gdx.audio.{Sound => GdxSound}

class SoundField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit)
  extends BrowseField[SoundSpec](owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new SoundSelectDialog(owner, sm, initial) {
      def onSuccess(result: Option[SoundSpec]) = 
        model = result
    }
    diag.open()
  }
}

class MusicField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit)
  extends BrowseField[SoundSpec](owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new SoundSelectDialog(owner, sm, initial) {
      def onSuccess(result: Option[SoundSpec]) = 
        model = result
    }
    diag.open()
  }
}

abstract class SoundSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec])
  extends ResourceSelectDialog(owner, sm, initial, true, Sound) {
    
  override def specToResourceName(spec: SoundSpec): String = spec.sound

  override def newRcNameToSpec(name: String,
                               prevSpec: Option[SoundSpec]): SoundSpec =
    prevSpec.getOrElse(SoundSpec("")).copy(sound = name)
  
  val playerPanel = new DesignGridPanel {
    val gdxPanel = new GdxPanel()
    
    var currentSound: Option[GdxSound] = None
    
    def loadSound(selection: SoundSpec) = {
      currentSound.map(_.dispose())
      
      val resource = Sound.readFromDisk(sm.getProj, selection.sound)
        
      currentSound = Some(gdxPanel.getAudio.newSound(resource.getHandle()))
    }
    
    row().grid().add(new Button(Action("Play") {
      currentSound.map(_.play())
    }))
    row().grid().add(gdxPanel)
  }
    
  override def rightPaneFor(
    selection: SoundSpec,
    updateSelectionF: SoundSpec => Unit): Component = {
    playerPanel.loadSound(selection)
    playerPanel
  }
}