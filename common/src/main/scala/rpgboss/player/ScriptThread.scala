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
import scala.collection.mutable.MutableList
import org.mozilla.javascript.debug.Debugger
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.mozilla.javascript.ContextFactory
import rpgboss.model.PictureSlots
import rpgboss.model.MusicSlots
import rpgboss.model.Constants

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
  extends FinishableByPromise
  with LazyLogging {
  def extraInitScope(jsScope: ScriptableObject): Unit = {}

  val runnable = new Runnable() {
    override def run() = {
      val (jsContext, jsScope) =
        ScriptHelper.enterGlobalContext(scriptInterface)

      extraInitScope(jsScope)

      try {
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
      } catch {
        case e: ThreadDeath =>
          System.err.println("Thread death")
        case e: org.mozilla.javascript.EcmaError => {
          System.err.println(e.getErrorMessage())
          System.err.println("%s:%d".format(e.sourceName(), e.lineNumber()))
        }
        case e: Throwable => e.printStackTrace()
      } finally {
        Context.exit()

        onFinish.map { f =>
          GdxUtils.syncRun {
            f()
          }
        }

        finish()
      }
    }
  }

  var thread: Thread = null

  def stop() = {
    // TODO: This is unsafe, but in practice, won't do anything bad... I think.
    if (thread != null)
      thread.stop()
  }

  def runOnNewThread() = {
    assert(thread == null)
    thread = new Thread(runnable)
    thread.start()
    this
  }

  def runOnSameThread() = {
    assert(thread == null)
    runnable.run()
  }
}

object ScriptHelper {
  def enterGlobalContext(
      scriptInterface: ScriptInterface): (Context, ScriptableObject) = {
    val jsContext = ContextFactory.getGlobal().enterContext()
    val jsScope = jsContext.initStandardObjects()

    def putProperty(objName: String, obj: Object) = {
      ScriptableObject.putProperty(jsScope, objName,
        Context.javaToJS(obj, jsScope))
    }

    putProperty("scalaScriptInterface", scriptInterface)

    putProperty("project", scriptInterface.project)
    putProperty("out", System.out)

    // Some models to be imported
    putProperty("Constants", Constants)
    putProperty("MapLoc", MapLoc)
    putProperty("Transitions", Transitions)
    putProperty("Keys", MyKeys)
    putProperty("MusicSlots", MusicSlots)
    putProperty("PictureSlots", PictureSlots)
    putProperty("None", None)

    val script = Script.readFromDisk(scriptInterface.project,
      ResourceConstants.globalsScript)
    jsContext.evaluateString(
      jsScope,
      script.readAsString,
      script.name,
      1, null)

    (jsContext, jsScope)
  }
}

class ScriptThreadFactory(scriptInterface: ScriptInterface) {
  def runFunction(fnToRun: String = "") = {
    val s = new ScriptThread(
    scriptInterface,
    scriptName = fnToRun,
    scriptBody = "",
    fnToRun)

    assert(scriptInterface.onBoundThread(),
        "Scripts should not spawn new threads when calling other scripts.")
    s.runOnNewThread()
  }

  def runFromFile(
    scriptName: String,
    fnToRun: String = "",
    onFinish: Option[() => Unit] = None,
    runOnNewThread: Boolean = true) = {
    val script = Script.readFromDisk(scriptInterface.project, scriptName)
    val s = new ScriptThread(
      scriptInterface,
      script.name,
      script.readAsString,
      fnToRun,
      onFinish)

    if (runOnNewThread) {
      assert(scriptInterface.onBoundThread(),
          "Scripts should not spawn new threads when calling other scripts.")
      s.runOnNewThread()
    } else {
      s.runOnSameThread()
    }

    s
  }

  def runFromEventEntity(
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

    val eventScriptBody = cmds.flatMap(_.toJs).mkString("\n")
    val scriptBody =
      """
      function eventScript() {
        %s
      }
      eventScript();
      """.format(eventScriptBody)
    val s = new ScriptThread(
      scriptInterface,
      scriptName,
      scriptBody,
      "",
      onFinish) {
      override def extraInitScope(jsScope: ScriptableObject) = {
        super.extraInitScope(jsScope)

        ScriptableObject.putProperty(jsScope, "event",
          Context.javaToJS(entity.getScriptInterface(), jsScope))
      }
    }
    s.runOnNewThread()
    s
  }
}