package rpgboss.player

import com.badlogic.gdx.backends.lwjgl.LwjglApplication

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.GL20

object Main {
  def main(args: Array[String]): Unit = {
    new LwjglApplication(new MyGame(), "player", 480, 320, false)
  }
}
