package rpgboss.player

import rpgboss.lib.Rect

case class Position(x: Float, y: Float)

class PositionProvider(val screenW: Int, val screenH: Int) {
  def proportional(xProportion: Float, yProportion: Float, w: Float, h: Float) =
    Position(xProportion * screenW, yProportion * screenH)

  def north(w: Float, h: Float) =
    Position(screenW / 2, h / 2)

  def south(w: Float, h: Float) =
    Position(screenW / 2, screenH - h / 2)

  def east(w: Float, h: Float) =
    Position(w / 2, screenH / 2)

  def west(w: Float, h: Float) =
    Position(screenW - w /2, screenH / 2)

  def northEast(w: Float, h: Float) =
    Position(screenW - w / 2, h / 2)

  def northWest(w: Float, h: Float) =
    Position(w / 2, h / 2)

  def southEast(w: Float, h: Float) =
    Position(screenW - w / 2, screenH - h / 2)

  def southWest(w: Float, h: Float) =
    Position(w / 2, screenH - h / 2)

  def centered(w: Float, h: Float) =
    Position(screenH / 2, screenW / 2)
}