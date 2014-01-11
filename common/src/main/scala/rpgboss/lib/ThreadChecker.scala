package rpgboss.lib

class ThreadChecker {
  private var _boundThread = java.lang.Thread.currentThread()
  def onValidThread() = java.lang.Thread.currentThread() == _boundThread 
}

trait ThreadChecked {
  private var _checker = new ThreadChecker
  def onValidThread() = _checker.onValidThread()
}