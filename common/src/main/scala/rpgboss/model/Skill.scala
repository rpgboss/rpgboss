package rpgboss.model

import com.typesafe.scalalogging.slf4j.Logging
import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.battle.BattleStatus

object DamageType extends RpgEnum {
  val Physical, Magic = Value
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

case class Damage(
  var typeId: Int = DamageType.Physical.id,
  var elementId: Int = 0,
  var formula: String = "") extends Logging {
  
  def getBaseDamage(source: BattleStatus, target: BattleStatus): Int = {
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
          
      val resultAsInt = Context.toNumber(jsResult).round.toInt
    
      Context.exit()
      
      resultAsInt
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
  
case class Skill(
  var name: String = "",
  var scopeId: Int = Scope.OneEnemy.id,
  var cost: Int = 0,
  var damages: Seq[Damage] = Seq(Damage()),
  var effects: Seq[Effect] = Seq()) extends HasName