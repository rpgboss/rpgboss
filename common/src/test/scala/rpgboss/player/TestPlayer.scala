package rpgboss.player

import java.io.File
import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import rpgboss.model.MapLoc

object TestPlayer {
  def launch(game: MyGame) = {
    val conf = new LwjglApplicationConfiguration();
    conf.title = game.project.data.title;
    conf.width = 32 * 20;
    conf.height = 32 * 15;
    conf.useGL20 = true;
    conf.forceExit = true;

    new LwjglApplication(game, conf);
  }
}

abstract class TestGame(gamepath: File) 
  extends MyGame(gamepath) {
  
  val script = ScriptThread.fromFile(
    this, 
    "main.js", 
    "initializeData()",
    onFinish = Some(onInitFinish _),
    onFinishOnScriptThread = true)
  
  override def beginGame() = {
    script.run()
  }
  
  def onInitFinish() = {
    state.setPlayerLoc(project.data.startup.startingLoc);
    runTest()
    exit()
  }
  
  def runTest()
  
  def awaitFinish() = script.awaitFinish()
}