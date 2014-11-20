package rpgboss.editor.resourceselector

import rpgboss.editor.StateMaster
import scala.swing._
import scala.swing.event._
import rpgboss.model.resource._
import rpgboss.model._
import rpgboss.editor.uibase._
import com.badlogic.gdx.audio.{ Sound => GdxSound, Music => GdxMusic }
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.Gdx

class SoundField(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  onUpdate: Option[SoundSpec] => Unit,
  allowNone: Boolean = true)
  extends BrowseField[SoundSpec](owner, sm, initial, onUpdate) {
  override def modelToString(x: SoundSpec) = x.sound
  def doBrowse() = {
    val diag = new SoundSelectDialog(owner, sm, model, allowNone) {
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
    val diag = new MusicSelectDialog(owner, sm, model) {
      def onSuccess(result: Option[SoundSpec]) =
        model = result
    }
    diag.open()
  }
}

abstract class SoundSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec],
  allowNone: Boolean = true)
  extends ResourceSelectDialog(owner, sm, initial, allowNone, Sound)
  with LazyLogging {

  override def specToResourceName(spec: SoundSpec): String = spec.sound

  override def newRcNameToSpec(name: String,
                               prevSpec: Option[SoundSpec]): SoundSpec =
    prevSpec.getOrElse(SoundSpec("")).copy(sound = name)

  override def rightPaneFor(
    selection: SoundSpec,
    updateSelectionF: SoundSpec => Unit) = {
    new DesignGridPanel with DisposableComponent {
      import rpgboss.editor.uibase.SwingUtils._

      override def dispose() = {
        currentSound.dispose()
        gdxPanel.dispose()
      }

      val gdxPanel = new GdxPanel(sm.getProj)

      val volumeSlider = floatSlider(
        selection.volume, 0f, 1f, 100, 5, 25,
        v => updateSelectionF(
          selection.copy(volume = v)))

      val pitchSlider = floatSlider(
        selection.pitch, 0.5f, 1.5f, 100, 5, 25,
        v => updateSelectionF(
          selection.copy(pitch = v)))

      val resource = Sound.readFromDisk(sm.getProj, selection.sound)

      val currentSound = gdxPanel.getAudio.newSound(resource.getGdxFileHandle)

      row().grid().add(new Button(Action("Play") {
        currentSound.play(volumeSlider.floatValue, pitchSlider.floatValue, 0f)
      }))

      row().grid(new Label("Volume:")).add(volumeSlider)
      row().grid(new Label("Pitch:")).add(pitchSlider)
      row().grid().add(gdxPanel)
    }
  }
}

abstract class MusicSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SoundSpec])
  extends ResourceSelectDialog(owner, sm, initial, true, Music)
  with LazyLogging {

  override def specToResourceName(spec: SoundSpec): String = spec.sound

  override def newRcNameToSpec(name: String,
                               prevSpec: Option[SoundSpec]): SoundSpec =
    prevSpec.getOrElse(SoundSpec("")).copy(sound = name)

  override def rightPaneFor(
    selection: SoundSpec,
    updateSelectionF: SoundSpec => Unit) = {
    new DesignGridPanel with DisposableComponent {
      import rpgboss.editor.uibase.SwingUtils._

      val assets = new RpgAssetManager(sm.getProj)

      override def dispose() = {
        currentMusic.map(_.dispose())
        gdxPanel.dispose()
        assets.dispose()
      }

      val volumeSlider = floatSlider(
        selection.volume, 0f, 1f, 100, 5, 25,
        v => {
          currentMusic.map(_.setVolume(v))
          updateSelectionF(selection.copy(volume = v))
        })

      val gdxPanel = new GdxPanel(sm.getProj)

      val resource = Music.readFromDisk(sm.getProj, selection.sound)

      val currentMusic: Option[MusicPlayer] = Some(resource.newPlayer(assets))

      row().grid().add(new Button(Action("Play") {
        currentMusic.map(_.stop())
        currentMusic.map(_.setVolume(volumeSlider.floatValue))
        currentMusic.map(_.play())
      }))

      row().grid(new Label("Volume:")).add(volumeSlider)
      row().grid().add(gdxPanel)
    }
  }
}