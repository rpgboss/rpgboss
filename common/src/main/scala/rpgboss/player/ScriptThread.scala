package rpgboss.player

import org.mozilla.javascript.{Context, ScriptableObject, Scriptable}
import rpgboss.model.resource.Script
import scala.concurrent.ops.spawn

case class ScriptThread(game: MyGame, scriptName: String, fnToRun: String = "") {
  def run() = spawn {
    val script = Script.readFromDisk(game.project, scriptName)
    val jsInterface = new ScriptInterface(game)
    
    val jsContext = Context.enter()
    val jsScope = jsContext.initStandardObjects()
    
    ScriptableObject.putProperty(jsScope, "game", 
        Context.javaToJS(jsInterface, jsScope))
    ScriptableObject.putProperty(jsScope, "out", 
        Context.javaToJS(System.out, jsScope))
    
    jsContext.evaluateString(
        jsScope, 
        script.getAsString,
        script.name,
        1, null)
    
    if(!fnToRun.isEmpty) {
      jsContext.evaluateString(
          jsScope,
          fnToRun,
          fnToRun,
          1, null)
    }
  }
}