package rpgboss.player
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input._
import com.badlogic.gdx.utils.Timer

trait InputHandler {
  import MyKeys._

  def keyDown(key: Int) = {}

  def keyUp(key: Int) = {}

  // Called when an input handler gets put ahead of this one, capturing a key
  def keyCapturedByOther(key: Int) = {}

  // Defines list of keys swallowed by this event handler
  val capturedKeys = Set(Up, Down, Left, Right, OK)
}

object MyKeys {
  val Up = 0
  val Down = 1
  val Left = 2
  val Right = 3
  val OK = 4
  val Cancel = 5

  val totalNumber = 6
}

trait MoveInputHandler extends InputHandler {
  // Initialized to all false
  private val isActiveMap = new Array[Boolean](5)

  def isActive(key: Int) = isActiveMap(key)

  override def keyDown(key: Int) = {
    isActiveMap(key) = true
  }

  override def keyUp(key: Int) = {
    isActiveMap(key) = false
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
  import MyKeys._
  def keyDelay: Float = 0.5f
  def keyInterval: Float = 0.1f

  // Defines list of keys swallowed by this event handler
  override val capturedKeys = Set(Up, Down, Left, Right, OK)

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
  }

  override def keyUp(key: Int) = {
    // Cancel task
    activateTasks(key).cancel()
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
    case Keys.SPACE => Some(MyKeys.OK)
    case _ => None
  }

  override def keyDown(keycode: Int) = mapKey(keycode) map { key =>
    /*
     * This bit of hackery iterates through the whole list looking for a
     * handler that handles the input correctly.
     */
    val handler = inputProcessors.find { _.capturedKeys.contains(key) }
    handler.map { _.keyDown(key) }.isDefined
  } getOrElse false

  override def keyUp(keycode: Int) = mapKey(keycode) map { key =>
    val handler = inputProcessors.find { _.capturedKeys.contains(key) }
    handler.map { _.keyUp(key) }.isDefined
  } getOrElse false

  def prepend(newHandler: InputHandler) = {
    // Send a signal to all existing input handlers, as if they keep track of
    // the 'down' position, they should know that it's been captured.
    for (handler <- inputProcessors; key <- newHandler.capturedKeys) {
      if (handler.capturedKeys.contains(key)) {
        handler.keyCapturedByOther(key)
      }
    }

    inputProcessors.prepend(newHandler)
  }

  def remove(handler: InputHandler) = {
    inputProcessors -= handler
  }
}