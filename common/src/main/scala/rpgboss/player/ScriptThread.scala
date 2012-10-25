package rpgboss.player

import org.mozilla.javascript.{Context, ScriptableObject, Scriptable}
import rpgboss.model.resource.Script
import scala.concurrent.ops.spawn
import rpgboss.model.MapLoc
import java.lang.Thread.UncaughtExceptionHandler

case class ScriptThread(game: MyGame, scriptName: String, fnToRun: String = "") 
  extends UncaughtExceptionHandler
{
  def run() = spawn {
    Thread.setDefaultUncaughtExceptionHandler(this)
    
    val script = Script.readFromDisk(game.project, scriptName)
    val jsInterface = game.state
    
    val jsContext = Context.enter()
    val jsScope = jsContext.initStandardObjects()
    
    ScriptableObject.putProperty(jsScope, "game", 
        Context.javaToJS(jsInterface, jsScope))
    ScriptableObject.putProperty(jsScope, "project", 
        Context.javaToJS(game.project, jsScope))
        
    ScriptableObject.putProperty(jsScope, "out", 
        Context.javaToJS(System.out, jsScope))
        
    // Some models to be imported
    ScriptableObject.putProperty(jsScope, "MapLoc", 
        Context.javaToJS(MapLoc, jsScope))
    
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
  
  def uncaughtException(thread: Thread, ex: Throwable) = {
    ex.printStackTrace()
  }
}