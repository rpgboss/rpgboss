package rpgboss.player

class StartScreen(val game: RpgGame) extends RpgScreen {
  override def render() = {
    windowManager.preMapRender()
    windowManager.render()
  }
  
  override def update(delta: Float) = {
    windowManager.update(delta)
  }
}