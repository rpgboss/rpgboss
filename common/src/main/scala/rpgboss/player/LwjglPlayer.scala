package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._

object LwjglPlayer {
  def launch(game: RpgGame) = {
    val conf = new LwjglApplicationConfiguration();
    conf.title = game.project.data.title;
    conf.width = game.project.data.startup.screenW;
    conf.height = game.project.data.startup.screenH;
    conf.forceExit = true;

    new LwjglApplication(game, conf);
  }

  def main(args: Array[String])  : Unit = {
    if (args.length < 1) {

    } else {
      val file = new File(args(0))

      if (!file.isDirectory()) {
        println("Cannot read directory: " + args(0))
        return
      }
      val game = new RpgGame(file)
      launch(game);
    }
  }
}