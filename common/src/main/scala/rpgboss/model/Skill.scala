package rpgboss.model

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.battle._
import rpgboss.lib._

object DamageType extends RpgEnum {
  val Physical, Magic, MPDamage, AddStatusEffect, RemoveStatusEffect = Value
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

/**
 * @param   elementId       Doesn't have meaning when damageType is a status
 *                          effect.
 * @param   value           Normally is the damage amount. However, in the
 *                          status effect case, is the status effect added.
 */
case class Damage(damageType: DamageType.Value, elementId: Int, value: Int) {
  import DamageType._
  def damageString(pData: ProjectData) = damageType match {
    case Physical => value.toString
    case Magic => value.toString
    case MPDamage => "-%s MP".format(value)
    case AddStatusEffect => pData.enums.statusEffects(value).name
    case RemoveStatusEffect => "-" + pData.enums.statusEffects(value).name
  }
}

case class DamageFormula(
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
                 skill: Skill): Array[Damage] = {
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

      Damage(
        DamageType.apply(damage.typeId), damage.elementId, damageValue)
    }
  }
}

case class Skill(
  var name: String = "",
  var scopeId: Int = Scope.OneEnemy.id,
  var cost: Int = 0,
  var damages: Array[DamageFormula] = Array(DamageFormula()),
  var effects: Array[Effect] = Array(),
  var animationId: Int = 0) extends HasName {
  def applySkill(actor: BattleStatus, target: BattleStatus): Seq[Hit] = {
    val allDamages = collection.mutable.ArrayBuffer[Damage]()

    // Apply direct damages
    val directDamages = Damage.getDamages(actor, target, this)
    target.hp -= directDamages.map(_.value).sum
    allDamages ++= directDamages

    // Apply effects and effect damages
    allDamages ++= effects.flatMap(_.applyAsSkillOrItem(target))

    target.clampVitals()

    allDamages.map(Hit(target, _, animationId))
  }
}