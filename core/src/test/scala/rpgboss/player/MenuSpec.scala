package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.player._
import rpgboss.player.entity._

class MenuSpec extends UnitSpec {
  "Menu" should "open and then close" in {
    val test = new MapScreenTest {
      override def dismissWaiterAtEndOfTestScript = false

      override def testScript() = {
        scriptInterface.syncRun {
          game.mapScreen.scriptFactory.runFromFile(
            "sys/menu.js",
            "menu()",
            Some(() => {
              waiter.dismiss()
            }))
        }

        // TODO: Fix hack maybe. Wait one second for menu to open.
        scriptInterface.sleep(1.0f)
        scriptInterface.mapScreenKeyPress(MyKeys.Cancel)
      }
    }

    test.runTest()
  }

  "Party Status Menu" should "loop correctly" in {
    val test = new MapScreenTest {
      override def dismissWaiterAtEndOfTestScript = false

      override def testScript() = {
        scriptInterface.modifyParty(true, 3)
        scriptInterface.modifyParty(true, 4)

        scriptInterface.syncRun {
          TestScriptThread.fromTestScript(
            game.mapScreen.scriptInterface,
            "menutest.js",
            "testStatusMenu()",
            waiter).runOnNewThread()
        }

        // TODO: Fix hack maybe. Wait one second for menu to open.
        scriptInterface.sleep(1.0f)
        scriptInterface.mapScreenKeyPress(MyKeys.OK)
        scriptInterface.sleep(0.5f)
        scriptInterface.mapScreenKeyPress(MyKeys.Down)
        scriptInterface.sleep(0.5f)
        scriptInterface.mapScreenKeyPress(MyKeys.OK)
      }
    }

    test.runTest()
  }
}
