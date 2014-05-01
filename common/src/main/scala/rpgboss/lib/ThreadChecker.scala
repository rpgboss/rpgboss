package rpgboss.lib

class ThreadChecker {
  private var _boundThread = java.lang.Thread.currentThread()
  def onBoundThread() = java.lang.Thread.currentThread() == _boundThread 
}

trait ThreadChecked {
  private var _checker = new ThreadChecker
  def onBoundThread() = _checker.onBoundThread()
  def onDifferentThread() = !onBoundThread()
}