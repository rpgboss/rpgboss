package rpgboss.player

import java.lang.Thread.UncaughtExceptionHandler
import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import rpgboss.lib.GdxUtils
import rpgboss.model.Transitions
import rpgboss.model.ItemAccessibility
import rpgboss.model.ItemType
import rpgboss.model.MapLoc
import rpgboss.model.event._
import rpgboss.model.resource.ResourceConstants
import rpgboss.model.resource.Script
import rpgboss.player.entity.EventEntity

/**
 * Thread used to run a javascript script...
 *
 * @param   game                  MyGame instance
 * @param   scriptName            Name of script used for debugging and logging
 * @param   scriptBody            Body of script. Should be broken into lines.
 * @param   fnToRun               Javascript to run after scriptBody is done.
 * @param   onFinish              Function to run on gdx thread after script end
 *
 */
class ScriptThread(
  scriptInterface: ScriptInterface,
  scriptName: String,
  scriptBody: String,
  fnToRun: String = "",
  onFinish: Option[() => Unit] = None)
  extends UncaughtExceptionHandler
  with FinishableByPromise {
  def initScope(jsScope: ScriptableObject): Any = {
    def putProperty(objName: String, obj: Object) = {
      ScriptableObject.putProperty(jsScope, objName,
          Context.javaToJS(obj, jsScope))
    }

    putProperty("game", scriptInterface)

    putProperty("project", scriptInterface.project)
    putProperty("out", System.out)

    // Some models to be imported
    putProperty("MapLoc", MapLoc)
    putProperty("Transitions", Transitions)
    putProperty("Keys", MyKeys)
    putProperty("None", None)
  }

  val runnable = new Runnable() {
    override def run() = {
      Thread.setDefaultUncaughtExceptionHandler(ScriptThread.this)

      val jsContext = Context.enter()
      val jsScope = jsContext.initStandardObjects()

      initScope(jsScope)

      val globalScript =
        Script.readFromDisk(scriptInterface.project,
            ResourceConstants.globalsScript)

      jsContext.evaluateString(
        jsScope,
        globalScript.readAsString,
        globalScript.name,
        1, null)

      jsContext.evaluateString(
        jsScope,
        scriptBody,
        scriptName,
        1, null)

      if (!fnToRun.isEmpty) {
        jsContext.evaluateString(
          jsScope,
          fnToRun,
          fnToRun,
          1, null)
      }

      Context.exit()

      onFinish.map { f =>
        GdxUtils.syncRun {
          f()
        }
      }

      finish()
    }
  }

  val customRunnable = new Runnable() {
    override def run() = {
      Thread.setDefaultUncaughtExceptionHandler(ScriptThread.this)

      val jsContext = Context.enter()
      val jsScope = jsContext.initStandardObjects()

      initScope(jsScope)

      val customScript =
        Script.readFromDisk(scriptInterface.project,
            ResourceConstants.customScript)

        
      jsContext.evaluateString(
        jsScope,
        customScript.readAsString,
        customScript.name,
        1, null)

      if (!fnToRun.isEmpty) {
        jsContext.evaluateString(
          jsScope,
          fnToRun,
          fnToRun,
          1, null)
      }

      Context.exit()

      onFinish.map { f =>
        GdxUtils.syncRun {
          f()
        }
      }

      finish()
    }
  }

  val thread = new Thread(runnable)
  val customThread = new Thread(customRunnable)

  def run() = {
    assert(!thread.isAlive())
    assert(!customThread.isAlive())
    thread.start()
    customThread.start()
    this
  }

  def uncaughtException(thread: Thread, ex: Throwable) = {
    ex match {
      case e: org.mozilla.javascript.EcmaError => {
        System.err.println(e.getErrorMessage())
        System.err.println("%s:%d".format(e.sourceName(), e.lineNumber()))
      }
      case e => e.printStackTrace()
    }
  }
}

trait ScriptThreadFactory {
  def runFromFile(
    scriptName: String,
    fnToRun: String = "",
    onFinish: Option[() => Unit] = None): Finishable

  def runFromEventEntity(
    entity: EventEntity,
    eventState: RpgEventState,
    state: Int,
    onFinish: Option[() => Unit] = None): Finishable
}

class GameScriptThreadFactory(scriptInterface: ScriptInterface)
extends ScriptThreadFactory {
  override def runFromFile(
    scriptName: String,
    fnToRun: String = "",
    onFinish: Option[() => Unit] = None) = {
    val script = Script.readFromDisk(scriptInterface.project, scriptName)
    val s = new ScriptThread(
      scriptInterface,
      script.name,
      script.readAsString,
      fnToRun,
      onFinish)
    s.run()
    s
  }

  override def runFromEventEntity(
    entity: EventEntity,
    eventState: RpgEventState,
    state: Int,
    onFinish: Option[() => Unit] = None) = {
    val extraCmdsAtEnd: Array[EventCmd] =
      if (eventState.runOnceThenIncrementState) {
        Array(IncrementEventState())
      } else {
        Array()
      }
    val cmds = eventState.cmds ++ extraCmdsAtEnd

    val scriptName = "%s/%d".format(entity.mapEvent.name, entity.evtStateIdx)

    val scriptBody = cmds.flatMap(_.toJs).mkString("\n")
    val s = new ScriptThread(
      scriptInterface,
      scriptName,
      scriptBody,
      "",
      onFinish) {
      override def initScope(jsScope: ScriptableObject) = {
        super.initScope(jsScope)

        ScriptableObject.putProperty(jsScope, "event",
            Context.javaToJS(entity.getScriptInterface(), jsScope))
      }
    }
    s.run()
    s
  }
}