package rpgboss.player

import com.badlogic.gdx.Game
import java.io.File
import com.badlogic.gdx.Screen
import rpgboss.model.Project

class MyGame(gamepath: File) extends Game {
  val project = Project.readFromDisk(gamepath).get
  
  var startScreen: GameStartScreen = null
  var gameScreen: GameMainScreen = null
  
  def create() = {
    startScreen = new GameStartScreen(this)
    gameScreen = new GameMainScreen(this)
    setScreen(startScreen)
  }
  
  override def dispose(): Unit = {
    startScreen.dispose()
    gameScreen.dispose()
  }

}