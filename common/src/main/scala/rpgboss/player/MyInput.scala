package rpgboss.player
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input._
import com.badlogic.gdx.utils.Timer

trait InputHandler {
  def keyDown(key: Int) : Boolean
  
  def keyUp(key: Int) : Boolean
}

object MyKeys {
  val Up = 0
  val Down = 1
  val Left = 2
  val Right = 3
  val A = 4
  
  val totalNumber = 5
}

/**
 * Simple one that just detects whether or not the key is down
 */
trait IsDownInputHandler extends InputHandler {
  // Initialized to all false
  private val downMap = new Array[Boolean](5)
  
  def keyDown(key: Int) = {
    downMap(key) = true
    true
  }
  
  def keyUp(key: Int) = {
    downMap(key) = false
    true
  }
  
  def down(key: Int) = downMap(key)
}

/**
 * The whole purpose of this input handler is to send one "activation"
 * of the key on initial key press, and then if it's still held down,
 * send repeated activations at regular intervals until the key goes up.
 * 
 * This is useful for scrolling through menu items.
 */
trait MenuInputHandler extends IsDownInputHandler {
  def keyDelay: Float
  def keyInterval: Float
  
  private val activateTasks = (0 until MyKeys.totalNumber) map { key =>
    new Timer.Task() {
      def run() = {
        keyActivate(key)
      }
    }
  }
  
  override def keyDown(key: Int) = {
    super.keyDown(key)
    
    // Initial activation
    keyActivate(key)
    
    // Schedule a task to be repeated
    Timer.schedule(activateTasks(key), keyDelay, keyInterval)
    true
  }
  
  override def keyUp(key: Int) = {
    super.keyUp(key)
    
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
  
  def prepend(handler: InputHandler) = {
    inputProcessors.prepend(handler)
  }
  
  def remove(handler: InputHandler) = {
    inputProcessors -= handler
  }
}