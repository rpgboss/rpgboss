package rpgboss.player
import rpgboss.player.entity._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import akka.dispatch.Await
import akka.util.Duration

/**
 * This class contains all the state information about the game.
 * 
 * It must ensure that different threads can mutate the state of the game
 * without causing concurrency errors.
 * 
 * Moreover, OpenGL operations may only occur on the GDX rendering thread.
 * This object must post those operations to that thread via postRunnable
 */
class GameState(game: MyGame, project: Project) {
  // No need for syncronization, since it's a synchronized collection
  val windows = new collection.mutable.ArrayBuffer[Window] 
      with collection.mutable.SynchronizedBuffer[Window]
  
  // Should only be accessed on the Gdx thread, so no synchronization needed
  val pictures = new Array[PictureInfo](32)
  
  // Should only be modified on the Gdx thread
  var curTransition: Option[Transition] = None
  
  // current map
  var mapAndAssetsOption : Option[MapAndAssets] = None
  
  // protagonist and camera position. Modify all these things on the Gdx thread
  val cameraLoc = new MutableMapLoc()
  var playerEvt: EventEntity = new PlayerEvent(game)
  
  // All the events on the current map, including the player event
  var allEvts = List[EventEntity](playerEvt)
  
  // Called every frame... by MyGame's render call. 
  def update(delta: Float) = {
    // Update events, including player event
    allEvts.foreach(_.update(delta))
    
    // Update windows
    if(!windows.isEmpty) 
      windows.head.update(delta, true)
    if(windows.length > 1)
      windows.tail.foreach(_.update(delta, false))
    
    // Update current transition
    curTransition.synchronized {
      curTransition map { transition =>
        if(transition.done) {
          curTransition = None
        }
      }
    }
    
    // Update camera location
    if(playerEvt.isMoving) {
      cameraLoc.x = playerEvt.x
      cameraLoc.y = playerEvt.y
    }
  }
  
  /**
   * Run the following on the GUI thread synchronously...
   */
  private def syncRun(op: => Any) = {
    val runnable = new Runnable() {
      def run() = op
    }
    Gdx.app.postRunnable(runnable)
  }
  
  /**
   * Calls the following on the GUI thread. Takes a frame's worth of time.
   */
  private def syncCall[T](op: => T): T = {
    val callable = new Callable[T]() {
      def call() = op
    }
    val future = new FutureTask(callable)
    
    Gdx.app.postRunnable(future)
    
    future.get
  }
  
  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    mapAndAssetsOption.map(_.dispose())
  }
  
  /*
   * The below functions are all called from the script threads only.
   */
  
  /*
   * Things to do with the player's location and camera
   */
  
  def setPlayerSprite(spritespec: SpriteSpec) = syncRun {
    playerEvt.setSprite(Some(spritespec))
  }
  
  def setPlayerLoc(loc: MapLoc) = syncRun {
    playerEvt.x = loc.x
    playerEvt.y = loc.y
  }
  
  def setCameraLoc(loc: MapLoc) = syncRun {
    cameraLoc.set(loc)
    
    // Update internal resources for the map
    if(cameraLoc.map.isEmpty()) {
      mapAndAssetsOption.map(_.dispose())
      mapAndAssetsOption = None
      allEvts = List(playerEvt)
    } else {
      mapAndAssetsOption.map(_.dispose())
      
      val mapAndAssets = new MapAndAssets(project, cameraLoc.map)
      mapAndAssetsOption = Some(mapAndAssets)
      allEvts = playerEvt :: mapAndAssets.mapData.events.map {
        new NonplayerEvent(game, _)
      }.toList
    }
  }
  
  /* 
   * Things to do with the screen
   */
  
  def setTransition(startAlpha: Float, endAlpha: Float, durationMs: Int) = syncRun {
    curTransition = Some(Transition(startAlpha, endAlpha, durationMs))
  }
  
  def showPicture(slot: Int, name: String, x: Int, y: Int, w: Int, h: Int) =  
    syncRun {
      pictures(slot) = PictureInfo(project, name, x, y, w, h)
    }
  
  def hidePicture(slot: Int) = syncRun {
    pictures(slot).dispose()
    pictures(slot) = null
  }
  
  /*
   * Things to do with user interaction
   */
  def sleep(durationMs: Int) = {
    Thread.sleep(durationMs)
  }
  
  def showChoices(
      choices: Array[String],
      x: Int, y: Int, w: Int, h: Int,
      justification: Int) : Int= {
    val window = new ChoiceWindow(
        game.assets, 
        project,
        choices,
        x, y, w, h,
        game.screenLayer.windowskin, 
        game.screenLayer.windowskinRegion, 
        game.screenLayer.fontbmp,
        initialState = Window.Opening,
        msPerChar = 0,
        justification = justification)
    
    windows.prepend(window)
    game.inputs.prepend(window)
    
    // Return the choice... eventually...
    val choice = Await.result(window.result.future, Duration.Inf)
    
    game.inputs.remove(window)
    windows -= window
    
    choice
  }
  
  def showTextWithPosition(
      text: Array[String],
      x: Int = 0, y: Int = 320, w: Int = 640, h: Int = 160) = {
    val window = new Window(
        game.assets,
        project,
        text,
        x, y, w, h,
        game.screenLayer.windowskin, 
        game.screenLayer.windowskinRegion, 
        game.screenLayer.fontbmp,
        initialState = Window.Opening)
    
    windows.prepend(window)
    game.inputs.prepend(window)
    
    Await.result(window.result.future, Duration.Inf)
    
    game.inputs.remove(window)
    windows -= window
  }
  
  def showText(text: Array[String]) = showTextWithPosition(text)
  
  val LEFT = Window.Left
  val CENTER = Window.Center
  val RIGHT = Window.Right
}

/**
 * Need call on dispose first
 */
case class PictureInfo(
    project: Project, 
    name: String, 
    x: Int, y: Int, w: Int, h: Int) {
  val picture = Picture.readFromDisk(project, name)
  val texture = 
    new Texture(Gdx.files.absolute(picture.dataFile.getAbsolutePath()))
  
  def dispose() = texture.dispose()
  
  def render(batch: SpriteBatch) = {
    batch.draw(texture,
        x, y, w, h,
        0, 0, texture.getWidth(), texture.getHeight(),
        false, true)
  }
}