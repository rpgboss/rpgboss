package rpgboss.model.event

import rpgboss.model.HasName
import rpgboss.model.RpgEnum
import rpgboss.model.ProjectData

object EventParameterType extends RpgEnum {
  val IntParam = Value(0)

  val AnimationId = Value(100)
  val CharacterId = Value(101)
  val CharClassId = Value(102)
  val ElementId = Value(103)
  val EnemyId = Value(104)
  val EncounterId = Value(105)
  val EquipTypeId = Value(106)
  val EventClassId = Value(107)
  val ItemId = Value(108)
  val SkillId = Value(109)
  val StatusEffectId = Value(110)

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