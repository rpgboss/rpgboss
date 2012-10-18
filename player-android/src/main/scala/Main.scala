package rpgboss.player

import android.os.Bundle

import com.badlogic.gdx.backends.android._

class Main extends AndroidApplication {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    val config = new AndroidApplicationConfiguration()
    config.useAccelerometer = false
    config.useCompass = false
    config.useWakelock = true
    config.depth = 0
    config.useGL20 = false
    initialize(new MyGame(), config)
  }
}
