package rpgboss.player;

import java.io.File;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import rpgboss.player.MyGame;

public class PlayerDesktop {

  public static LwjglApplication launch(String gamePath, boolean forceExit) {
    MyGame game = new MyGame(new File(gamePath));

    LwjglApplicationConfiguration conf = new LwjglApplicationConfiguration();
    conf.title = game.project().data().title();
    conf.width = 32 * 20;
    conf.height = 32 * 15;
    conf.useGL20 = true;
    conf.forceExit = forceExit;

    return new LwjglApplication(game, conf);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length < 1) {

    } else {
      launch(args[0], true);
    }

  }

}
