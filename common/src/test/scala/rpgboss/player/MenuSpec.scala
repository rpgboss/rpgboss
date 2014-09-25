package rpgboss.player

import rpgboss._
import rpgboss.model._
import rpgboss.player._
import rpgboss.player.entity._

class MenuSpec extends UnitSpec {

  "Menu" should "open and then close" in {
    val test = new MapScreenTest {
      override def dismissWaiterAtEndOfTestScript = false

      def testScript() = {
        val script = scriptInterface.syncRun {
          ScriptThread.fromFile(
            game,
            game.mapScreen,
            game.mapScreen.scriptInterface,
            "sys/menu.js",
            "menu()",
            Some(() => {
              waiter.dismiss()
            })).run()
        }

        // TODO: Fix hack maybe. Wait one second for menu to open.
        scriptInterface.sleep(1.0f)
        game.mapScreenKeyPress(MyKeys.Cancel)
      }
    }

    test.runTest()
  }
}
