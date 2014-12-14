package rpgboss.editor.uibase

import rpgboss.lib.Layout
import rpgboss.lib.Utils
import rpgboss.editor.Internationalized._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import rpgboss.lib.SizeType
import rpgboss.lib.LayoutType

class PixelPercentField(initial: Float, updateF: Float => Unit,
    allowNegative: Boolean)
  extends BoxPanel(Orientation.Vertical) {
  val pxMax = 9999
  val percentMax = 16f
  val pxMin = if (allowNegative) -pxMax else 1
  val percentMin = if (allowNegative) -percentMax else 0
  val fPx = pxField(pxMin, pxMax, initial.round, v => updateF(v))
  val fPercent = percentField(percentMin, percentMax, initial, updateF)

  override def enabled_=(value: Boolean) = {
    super.enabled = value
    fPx.enabled = value
    fPercent.enabled = value
  }

  def enterMode(currentValue: Float, pixelMode: Boolean) = {
    contents.clear()
    if (pixelMode) {
      fPx.setValue(Utils.clamped(currentValue, pxMin, pxMax))
      contents += fPx
    } else {
      fPercent.setValue(Utils.clamped(currentValue, percentMin, percentMax))
      contents += fPercent
    }
    revalidate()
    repaint()
  }
}

class LayoutEditingPanel(model: Layout) extends DesignGridPanel {
  val fSizeTypeId =
    enumIdCombo(SizeType)(model.sizeTypeId, model.sizeTypeId = _, Some(switchMode))

  val fLayoutTypeId =
    enumIdCombo(LayoutType)(model.layoutTypeId, model.layoutTypeId = _)

  val fW = new PixelPercentField(model.w, model.w = _, false)
  val fH = new PixelPercentField(model.h, model.h = _, false)
  val fXOffset = new PixelPercentField(model.xOffset, model.xOffset = _, true)
  val fYOffset = new PixelPercentField(model.yOffset, model.yOffset = _, true)

  def switchMode() = {
    val pixelMode = model.sizeTypeId == SizeType.Fixed.id
    val sizeEnabled = (
        model.sizeTypeId == SizeType.Fixed.id ||
        model.sizeTypeId == SizeType.ProportionalToScreen.id ||
        model.sizeTypeId == SizeType.ScaleSource.id)
    fW.enterMode(model.w, pixelMode)
    fH.enterMode(model.h, pixelMode)
    fXOffset.enterMode(model.xOffset, pixelMode)
    fYOffset.enterMode(model.xOffset, pixelMode)

    fW.enabled = sizeEnabled
    fH.enabled = sizeEnabled
  }
  switchMode()

  row().grid(lbl(needsTranslation("Size Type:"))).add(fSizeTypeId)
  row().grid(lbl(needsTranslation("Width:"))).add(fW)
  row().grid(lbl(needsTranslation("Height:"))).add(fH)
  row().grid(lbl(needsTranslation("Position:"))).add(fLayoutTypeId)
  row().grid(lbl(needsTranslation("X Offset"))).add(fXOffset)
  row().grid(lbl(needsTranslation("Y Offset"))).add(fYOffset)
}