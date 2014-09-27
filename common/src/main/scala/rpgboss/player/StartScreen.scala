package rpgboss.player

class StartScreen(val game: RpgGame) extends RpgScreenWithGame {
  override def render(): Unit = {
    if (!game.assets.update())
      return

    windowManager.preMapRender()
    windowManager.render()
  }

  override def update(delta: Float): Unit = {
    if (!game.assets.update())
      return

    windowManager.update(delta)
  }
}