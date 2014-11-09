package rpgboss.model.event

import rpgboss.model.HasName
import rpgboss.model.RpgEnum
import rpgboss.model.ProjectData

object EventParameterType extends RpgEnum {
  val IntParam, AnimationId, CharacterId, CharClassId, ElementId, EnemyId,
    EncounterId, EquipTypeId, ItemId, SkillId, StatusEffectId = Value

  val default = IntParam
}

case class EventParameter(paramTypeId: Int, intValue: Int)

case class RpgEventInstance(
  var eventClassId: Int,
  id: Int,
  var name: String,
  var x: Float,
  var y: Float,
  var params: Map[String, EventParameter])