package rpgboss.lib

trait ThreadChecked {
  private var _boundThread = java.lang.Thread.currentThread()
  
  def rebindToCurrentThread() = {
    _boundThread = java.lang.Thread.currentThread()
  }
  def assertOnBoundThread() = {
    assert(java.lang.Thread.currentThread() == _boundThread)
  }
  def assertOnDifferentThread() = {
    assert(java.lang.Thread.currentThread() != _boundThread)
  }
}