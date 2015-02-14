package rpgboss.player

class StartScreen(val game: RpgGame) extends RpgScreenWithGame {
  override def render(): Unit = {
    windowManager.preMapRender(batch, screenCamera)
    animationManager.render(batch, shapeRenderer, screenCamera)
    windowManager.render(batch, shapeRenderer, screenCamera)
  }

  override def update(delta: Float): Unit = {
  }
}