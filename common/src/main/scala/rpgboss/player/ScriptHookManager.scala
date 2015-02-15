package rpgboss.player

import org.mozilla.javascript.Context

class ScriptHookManager(scriptInterface: ScriptInterface) {
  def hookPeriod = 1.0f

  private var _timeSinceLastExecution = 0.0f

  val (jsContext, jsScope) = ScriptHelper.enterGlobalContext(scriptInterface)

  private val _scriptHooks =
    collection.mutable.Set[org.mozilla.javascript.Function]()

  def addScriptHook(jsFunction: org.mozilla.javascript.Function) = {
    _scriptHooks.add(jsFunction)
  }

  def update(delta: Float): Unit = {
    _timeSinceLastExecution += delta

    if (_timeSinceLastExecution <= hookPeriod)
      return

    _timeSinceLastExecution = 0

    try {
      val toRemove =
          collection.mutable.Set[org.mozilla.javascript.Function]()

      for (hook <- _scriptHooks) {
        val result =
          hook.call(jsContext, hook.getParentScope(), null, Array[Object]())

        val repeat = Context.toBoolean(result)

        if (!repeat) {
          toRemove.add(hook)
        }
      }

      toRemove.foreach(_scriptHooks.remove)

    } catch {
      case e: org.mozilla.javascript.EcmaError => {
        System.err.println(e.getErrorMessage())
        System.err.println("%s:%d".format(e.sourceName(), e.lineNumber()))
      }
    }
  }

  def dispose() = {
    Context.exit()
  }
}