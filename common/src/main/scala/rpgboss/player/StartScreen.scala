package rpgboss.player

class StartScreen(val game: RpgGame) extends RpgScreen {
  override def onFirstShow() = {
    ScriptThread.fromFile(
      game,
      scriptInterface,
      "start.js",
      "start()").run()
  }
  
  override def render() = {
    windowManager.render()
  }
  
  override def update(delta: Float) = {
    
  }
}