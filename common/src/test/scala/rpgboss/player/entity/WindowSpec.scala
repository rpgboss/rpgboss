package rpgboss.player.entity

import rpgboss._
import rpgboss.player._
import rpgboss.lib.Rect
import rpgboss.lib.Layout

class WindowSpec extends UnitSpec {
  "Window" should "do text substitution" in {
    val p = new PersistentState
    p.setInt("a", 1)
    p.setStringArray(ScriptInterfaceConstants.CHARACTER_NAMES ,
        Array("NameA", "NameB"))

    WindowText.processText(Array("hello", "world"), p) should equal (
        Array("hello", "world"))

    WindowText.processText(Array(raw"hello \N[0]", "world"), p) should equal (
        Array("hello NameA", "world"))

    WindowText.processText(Array(raw"hello \N[1] \V[a]"), p) should equal (
        Array("hello NameB 1"))

    WindowText.processText(Array(raw"hello \N[5] \V[a]"), p) should equal (
        Array("hello NAME_OUT_OF_BOUNDS 1"))

    WindowText.processText(Array(raw"hello \N[1] \V[b]"), p) should equal (
        Array("hello NameB 0"))
  }

  "Window" should "change state on time" in {
    val w = new Window(null, null, Layout.dummy) {
      override def openCloseTime = 0.25
    }
    w.state should equal (Window.Opening)
    w.update(0.30f)
    w.state should equal (Window.Open)
    w.startClosing()
    w.state should equal (Window.Closing)
    w.update(0.30f)
    w.state should equal (Window.Closed)
  }
}