package rpgboss.player

import aurelienribon.tweenengine._
import rpgboss.model.resource.MusicPlayer

class MusicPlayerTweenable(val musicPlayer: MusicPlayer)

class GdxMusicAccessor extends TweenAccessor[MusicPlayerTweenable] {
  import GdxMusicAccessor._

  def getValues(target: MusicPlayerTweenable, tweenType: Int,
                returnValues: Array[Float]): Int = {
    tweenType match {
      case VOLUME =>
        returnValues(VOLUME) = target.musicPlayer.getVolume()
        return 1
    }

    return 0
  }

  def setValues(target: MusicPlayerTweenable, tweenType: Int,
                newValues: Array[Float]) = {
    tweenType match {
      case VOLUME =>
        target.musicPlayer.setVolume(newValues(0))
    }
  }
}

object GdxMusicAccessor {
  val VOLUME = 0;
}

object TweenAccessors {
  def registerAccessors() = {
    Tween.registerAccessor(classOf[MusicPlayerTweenable], new GdxMusicAccessor())
  }
}