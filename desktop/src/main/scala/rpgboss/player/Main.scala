package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx._
import com.badlogic.gdx.graphics.GL20
import rpgboss.player.MyGame

object Main {
  def main(args: Array[String]): Unit = {
    val gamePath = new File(args(0))
    
    val game = new MyGame(gamePath) 
    
    val app = new LwjglApplication(
        game, 
        game.project.data.title, 
        32*20*2, 32*15*2, true)
  }
}
