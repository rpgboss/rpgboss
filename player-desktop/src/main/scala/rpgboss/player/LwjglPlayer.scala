package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._

object LwjglPlayer {
  def launch(game: MyGame) = {
    val conf = new LwjglApplicationConfiguration();
    conf.title = game.project.data.title;
    conf.width = 32 * 20;
    conf.height = 32 * 15;
    conf.useGL20 = true;
    conf.forceExit = true;

    new LwjglApplication(game, conf);
  }

  def main(args: Array[String])  : Unit = {
    if (args.length < 1) {

    } else {
      val game = new MyGame(new File(args(0)))
      launch(game);
    }
  }
}