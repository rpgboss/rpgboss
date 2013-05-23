package rpgboss.player

import aurelienribon.tweenengine._
import com.badlogic.gdx.audio.{ Music => GdxMusic }

class GdxMusicTweenable(val gdxMusic: GdxMusic)

class GdxMusicAccessor extends TweenAccessor[GdxMusicTweenable] {
  import GdxMusicAccessor._

  def getValues(target: GdxMusicTweenable, tweenType: Int,
                returnValues: Array[Float]): Int = {
    tweenType match {
      case VOLUME =>
        returnValues(VOLUME) = target.gdxMusic.getVolume()
        return 1
    }

    return 0
  }

  def setValues(target: GdxMusicTweenable, tweenType: Int,
                newValues: Array[Float]) = {
    tweenType match {
      case VOLUME =>
        target.gdxMusic.setVolume(newValues(0))
    }
  }
}

object GdxMusicAccessor {
  val VOLUME = 0;
}

object TweenAccessors {
  def registerAccessors() = {
    Tween.registerAccessor(classOf[GdxMusicTweenable], new GdxMusicAccessor())
  }
}