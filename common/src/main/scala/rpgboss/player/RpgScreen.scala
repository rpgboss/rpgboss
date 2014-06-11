package rpgboss.player

import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.{ Music => GdxMusic }
import com.badlogic.gdx.Gdx
import rpgboss.lib.ThreadChecked


trait RpgScreen extends Screen with ThreadChecked {
  def inputs: InputMultiplexer
  def createWindowManager(): WindowManager

  val musics = Array.fill[Option[GdxMusic]](8)(None)
  val windowManager = createWindowManager()
  
  def update(delta: Float)
  def render()

  override def dispose() = {
    windowManager.dispose()
    
    musics.foreach(_.map(music => {
      music.stop()
      music.dispose()
    }))
  }
  
  override def hide() = {
    assertOnBoundThread()
    Gdx.input.setInputProcessor(null)
    
    musics.foreach(_.map(_.pause()))
  }
   
  override def pause() = {
    assertOnBoundThread()
  }
  
  override def render(delta: Float) = {
    assertOnBoundThread()
    update(delta)
    render()
  }

  override def resize(width: Int, height: Int) = {
    assertOnBoundThread()
    // Do nothing for now
  }
  
  override def resume() = {
    assertOnBoundThread()
  }
  
  override def show() = {
    assertOnBoundThread()
    Gdx.input.setInputProcessor(inputs)
    
    musics.foreach(_.map(_.play()))
  }
}
