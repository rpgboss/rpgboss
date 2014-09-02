package rpgboss.model

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.battle._

object DamageType extends RpgEnum {
  val Physical, Magic, MPDamage = Value
  def default = Physical
}

class JSBattleEntity(status: BattleStatus) extends ScriptableObject {
  def getClassName = classOf[JSBattleEntity].getName()

  override def get(name: String, start: Scriptable): java.lang.Integer = {
    name match {
      case "hp" => status.hp
      case "mp" => status.mp
      case "mhp" => status.stats.mhp
      case "mmp" => status.stats.mmp
      case "atk" => status.stats.atk
      case "spd" => status.stats.spd
      case "mag" => status.stats.mag
      case "arm" => status.stats.arm
      case "mre" => status.stats.mre
      case _ => -1
    }
  }
}

case class TakenDamage(damageType: DamageType.Value, elementId: Int, value: Int)

case class Damage(
  var typeId: Int = DamageType.Physical.id,
  var elementId: Int = 0,
  var formula: String = "") extends LazyLogging {

  def getBaseDamage(source: BattleStatus, target: BattleStatus): Double = {
    val jsContext = Context.enter()
    val jsScope = jsContext.initStandardObjects()

    ScriptableObject.putProperty(
      jsScope, "a", Context.javaToJS(new JSBattleEntity(source), jsScope))
    ScriptableObject.putProperty(
      jsScope, "b", Context.javaToJS(new JSBattleEntity(target), jsScope))

    try {
      val jsResult = jsContext.evaluateString(
        jsScope,
        formula,
        "DamageFormula: %s".format(formula),
        1,
        null)

      val result = Context.toNumber(jsResult)

      Context.exit()

      result
    } catch {
      case e: Throwable => {
        logger.error(
          "Error while calculating damage with formula: %s. ".format(formula) +
  		  "Error: %s".format(e.getMessage()))
  		-1
      }
    }
  }
}

object Damage {
  def getDamages(source: BattleStatus, target: BattleStatus,
                 skill: Skill): Array[TakenDamage] = {
    import DamageType._

    for (damage <- skill.damages) yield {
      val armorOrMagicResist =
        if (damage.typeId == Physical.id) target.stats.arm else target.stats.mre

      val elementResist =
        if (damage.elementId < target.stats.elementResists.length)
          target.stats.elementResists(damage.elementId)
        else
          0

      val totalResist = armorOrMagicResist + elementResist

      val resistMultiplier = 1.0 / (1.0 + (totalResist.toDouble / 100.0))

      val baseDamage = damage.getBaseDamage(source, target)

      val damageValue = (baseDamage * resistMultiplier).round.toInt

      TakenDamage(
        DamageType.apply(damage.typeId), damage.elementId, damageValue)
    }
  }
}

case class Skill(
  var name: String = "",
  var scopeId: Int = Scope.OneEnemy.id,
  var cost: Int = 0,
  var damages: Array[Damage] = Array(Damage()),
  var effects: Array[Effect] = Array(),
  var animationId: Int = 0) extends HasName {
  def applySkill(actor: BattleStatus, target: BattleStatus) = {
    import EffectKey._

    val hits = new collection.mutable.ArrayBuffer[Hit]

    // Apply damages
    val damages = Damage.getDamages(actor, target, this)
    if (!damages.isEmpty) {
      hits.append(Hit(target, damages, animationId))

      target.hp -= damages.map(_.value).sum
    }

    // Apply other effects
    for (effect <- effects) {
      if (effect.keyId == RecoverHpAdd.id && target.hp > 0) {
        target.hp += effect.v1
        hits.append(
          Hit(target, Array(TakenDamage(DamageType.Magic, 0, -effect.v1)),
              animationId))
      }
    }

    if (target.hp < 0)
      target.hp = 0
    else if (target.hp > target.stats.mhp)
      target.hp = target.stats.mhp

    hits
  }
}