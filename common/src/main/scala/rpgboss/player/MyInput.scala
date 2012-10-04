package rpgboss.player
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input._
import com.badlogic.gdx.utils.Timer

trait InputHandler {
  import MyKeys._
  
  def keyDown(key: Int): Boolean = true
  
  def keyUp(key: Int): Boolean = true
  
  // Called when an input handler gets put ahead of this one, capturing a key
  def keyCapturedByOther(key: Int) = {}
  
  def capturedKeys = List(Up, Down, Left, Right, A)
}

object MyKeys {
  val Up = 0
  val Down = 1
  val Left = 2
  val Right = 3
  val A = 4
  
  val totalNumber = 5
}

trait MoveInputHandler extends InputHandler {
  // Initialized to all false
  private val isActiveMap = new Array[Boolean](5)
  
  def isActive(key: Int) = isActiveMap(key)
  
  override def keyDown(key: Int) = {
    isActiveMap(key) = true
    true
  }
  
  override def keyUp(key: Int) = {
    isActiveMap(key) = false
    true
  }
  
  override def keyCapturedByOther(key: Int) = {
    isActiveMap(key) = false
  }
}

/**
 * The whole purpose of this input handler is to send one "activation"
 * of the key on initial key press, and then if it's still held down,
 * send repeated activations at regular intervals until the key goes up.
 * 
 * This is useful for scrolling through menu items.
 */
trait ChoiceInputHandler extends InputHandler {
  def keyDelay: Float = 0.5f
  def keyInterval: Float = 0.1f
  
  private val activateTasks = (0 until MyKeys.totalNumber) map { key =>
    new Timer.Task() {
      def run() = {
        keyActivate(key)
      }
    }
  }
  
  override def keyDown(key: Int) = {
    // Initial activation
    keyActivate(key)
    
    // Schedule a task to be repeated 
    Timer.schedule(activateTasks(key), keyDelay, keyInterval)
    true
  }
  
  override def keyUp(key: Int) = {
    // Cancel task
    activateTasks(key).cancel()
    true
  }
  
  def keyActivate(key: Int)
}

/**
 * As in libgdx, delegation stops once a handler returns true
 */
class MyInputMultiplexer extends InputAdapter {
  val inputProcessors = new scala.collection.mutable.ListBuffer[InputHandler]()
  
  
  // Maps 
  def mapKey(keycode: Int): Option[Int] = keycode match {
    case Keys.UP => Some(MyKeys.Up)
    case Keys.DOWN => Some(MyKeys.Down)
    case Keys.LEFT => Some(MyKeys.Left)
    case Keys.RIGHT => Some(MyKeys.Right)
    case Keys.SPACE => Some(MyKeys.A)
    case _ => None
  }
  
  override def keyDown(keycode: Int) = mapKey(keycode) map { key =>
    /*
     * This bit of hackery iterates through the whole list looking for a
     * handler that handles the input correctly.
     */
    inputProcessors.find { handler =>
      handler.keyDown(key)
    } isDefined // returns false if it doesn't find anything
  } getOrElse false
  
  override def keyUp(keycode: Int) = mapKey(keycode) map { key =>
    inputProcessors.find { handler =>
      handler.keyUp(key)
    } isDefined
  } getOrElse false
  
  def prepend(newHandler: InputHandler) = {
    // Send a signal to all existing input handlers, as if they keep track of
    // the 'down' position, they should know that it's been captured.
    for(handler <- inputProcessors; key <- newHandler.capturedKeys)
      handler.keyCapturedByOther(key)
    
    inputProcessors.prepend(newHandler)
  }
  
  def remove(handler: InputHandler) = {
    inputProcessors -= handler
  }
}