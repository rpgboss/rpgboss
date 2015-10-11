package rpgboss.player
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input._
import com.badlogic.gdx.utils.Timer

trait InputHandler {
  import MyKeys._

  def keyDown(key: Int) = {}
  def keyUp(key: Int) = {}

  // Defines list of keys swallowed by this event handler
  def capturedKeys = Set(Up, Down, Left, Right, OK, Cancel)
}

class OneTimeInputHandler(capturedKeysArgument: Set[Int])
  extends InputHandler
  with FinishableByPromise {
  override def keyDown(key: Int) = {
    finishWith(key)
  }

  override def capturedKeys = {
    if (isFinished) {
      Set.empty
    } else {
      capturedKeysArgument
    }
  }
}

trait PlayerInputHandler extends InputHandler {
  private val keyIsActiveMap = new Array[Boolean](MyKeys.totalNumber)

  def keyIsActive(key: Int) = keyIsActiveMap(key)

  override def keyDown(key: Int) = {
    keyIsActiveMap(key) = true
    keyActivated(key)
  }
  override def keyUp(key: Int) = {
    keyIsActiveMap(key) = false
    keyDeactivated(key)
  }

  def keyActivated(key: Int)
  def keyDeactivated(key: Int)
}

object MyKeys {
  val Up = 0
  val Down = 1
  val Left = 2
  val Right = 3
  val OK = 4
  val Cancel = 5
  val Special1 = 6
  val Special2 = 7
  val Start = 8
  val Select = 9

  val totalNumber = 10
}

object MyKeysEnum extends Enumeration {
  val Up = Value(0, "Up")
  val Down = Value(1, "Down")
  val Left = Value(2, "Left")
  val Right = Value(3, "Right")
  val OK = Value(4, "OK")
  val Cancel = Value(5, "Cancel")
  val Special1 = Value(6, "Special1")
  val Special2 = Value(7, "Special2")
  val Start = Value(8, "Start")
  val Select = Value(9, "Select")

  def keysNames = values.toArray.map { value =>
    rpgboss.model.HasName.StringToHasName(value.toString())
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
    activateTasks(key).cancel()
  }

  def keyActivate(key: Int)
}

/**
 * As in libgdx, delegation stops once a handler returns true
 */
class InputMultiplexer extends InputAdapter {
  val inputProcessors = new scala.collection.mutable.ListBuffer[InputHandler]()

  def hasFocus(handler: InputHandler) =
    !inputProcessors.isEmpty && inputProcessors.head == handler

  def releaseAllKeys() = {
    for (i <- 0 until MyKeys.totalNumber; if keyIsActive(i)) {
      myKeyUp(i)
    }
  }

  // Maps
  def mapKey(keycode: Int): Option[Int] = keycode match {
    case Keys.UP => Some(MyKeys.Up)
    case Keys.DOWN => Some(MyKeys.Down)
    case Keys.LEFT => Some(MyKeys.Left)
    case Keys.RIGHT => Some(MyKeys.Right)
    case Keys.SPACE => Some(MyKeys.OK)
    case Keys.M => Some(MyKeys.Cancel)
    case Keys.X => Some(MyKeys.Cancel)
    case Keys.ESCAPE => Some(MyKeys.Cancel)
    case Keys.CONTROL_LEFT => Some(MyKeys.Special1)
    case Keys.SHIFT_LEFT => Some(MyKeys.Special2)
    case Keys.ALT_LEFT => Some(MyKeys.Start)
    case Keys.A => Some(MyKeys.Select)
    case _ => None
  }

  private val keyIsActive = new Array[Boolean](MyKeys.totalNumber)

  override def keyDown(keycode: Int) =
    mapKey(keycode).map(myKeyDown _).getOrElse(false)

  // Key down on abstract keys defined internally
  def myKeyDown(key: Int) = {
    /*
     * This bit of hackery iterates through the whole list looking for a
     * handler that handles the input correctly.
     */
    keyIsActive(key) = true
    val handler = inputProcessors.find { _.capturedKeys.contains(key) }
    handler.map { _.keyDown(key) }.isDefined
  }

  override def keyUp(keycode: Int) =
    mapKey(keycode).map(myKeyUp _).getOrElse(false)

  def myKeyUp(key: Int) = {
    keyIsActive(key) = false
    val handler = inputProcessors.find { _.capturedKeys.contains(key) }
    handler.map { _.keyUp(key) }.isDefined
  }

  def prepend(newHandler: InputHandler) = {
    for (key <- newHandler.capturedKeys; if keyIsActive(key)) {
      // Send a keyUp signals to all input handlers that are now shadowed
      for (handler <- inputProcessors) {
        if (handler.capturedKeys.contains(key))
          handler.keyUp(key)
      }
    }

    inputProcessors.prepend(newHandler)
  }

  def remove(handler: InputHandler) = {
    for (key <- handler.capturedKeys; if keyIsActive(key)) {
      // Send a keyUp to the handler before removal
      handler.keyUp(key)
    }

    inputProcessors -= handler
  }
}