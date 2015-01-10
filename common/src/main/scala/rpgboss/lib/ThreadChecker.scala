package rpgboss.lib

trait ThreadChecked {
  private var _boundThread = java.lang.Thread.currentThread()

  def rebindToCurrentThread() = {
    _boundThread = java.lang.Thread.currentThread()
  }
  def assertOnBoundThread() = {
    assert(onBoundThread())
  }
  def assertOnDifferentThread() = {
    assert(!onBoundThread())
  }
  def onBoundThread() = {
    java.lang.Thread.currentThread() == _boundThread
  }
}