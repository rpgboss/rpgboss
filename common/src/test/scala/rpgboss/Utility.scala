package rpgboss

import rpgboss.player.DummyFinished
import rpgboss.player.ScriptThreadFactory
import rpgboss.player.entity.EventEntity
import rpgboss.model.event.RpgEventState

class FakeScriptThreadFactory() extends ScriptThreadFactory {
  override def runFromFile(
    scriptName: String,
    fnToRun: String = "",
    onFinish: Option[() => Unit] = None) = {
    onFinish.map(_())
    DummyFinished
  }

  override def runFromEventEntity(
    entity: EventEntity,
    eventState: RpgEventState,
    state: Int,
    onFinish: Option[() => Unit] = None) = {
    onFinish.map(_())
    DummyFinished
  }
}