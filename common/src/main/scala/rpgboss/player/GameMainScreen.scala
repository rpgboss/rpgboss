package rpgboss.player

import com.badlogic.gdx.Screen
import java.io.File
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.utils.Logger
import rpgboss.player.entity._

class MutableMapLoc(var map: Int = -1, var x: Float = 0, var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)
  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

class GameMainScreen(game: MyGame)  {
  val project = game.project
  
}
