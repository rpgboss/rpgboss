package rpgboss.model

import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.battle.BattleStatus

object DamageType extends RpgEnum {
  val Physical, Magic = Value
  def default = Physical
}

class JSBattleEntity(hp: Int, atk: Int) extends ScriptableObject {
  /**
   * No-arg constructor used to construct the Javascript prototype object.
   */
  def this() = { this(0, 0) }
  
  def getClassName = classOf[JSBattleEntity].getName()

  def jsGet_hp() = hp
  def jsGet_atk() = atk
}

object JSBattleEntity {
  def apply(x: BattleStatus) = new JSBattleEntity(
      x.hp,
      x.stats.atk)
}

case class Damage(
  var typeId: Int = DamageType.Physical.id,
  var elementId: Int = 0,
  var formula: String = "") {
  
  def getBaseDamage(source: BattleStatus, target: BattleStatus) = {
    val jsContext = Context.enter()
    val jsScope = jsContext.initStandardObjects()
    ScriptableObject.defineClass(jsScope, classOf[JSBattleEntity])
    
    ScriptableObject.putProperty(
      jsScope, "a", Context.javaToJS(source, jsScope))
    ScriptableObject.putProperty(
      jsScope, "b", Context.javaToJS(target, jsScope))
      
    val jsResult = jsContext.evaluateString(
      jsScope, 
      formula, 
      "DamageFormula: %s".format(formula),
      1, 
      null)
    
    println(jsResult)
    
    val resultAsInt = Context.toNumber(jsResult).round.toInt
  
    Context.exit()
    
    resultAsInt
  }
}
  
case class Skill(
  var name: String = "",
  var scopeId: Int = Scope.OneEnemy.id,
  var cost: Int = 0,
  var damages: Seq[Damage] = Seq(Damage()),
  var effects: Seq[Effect] = Seq()) extends HasName