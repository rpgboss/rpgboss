package rpgboss.editor.dialog.db.components

import scala.collection.mutable.ArrayBuffer
import scala.swing.BoxPanel
import scala.swing.Dimension
import scala.swing.Orientation
import scala.swing.Window

import javax.swing.BorderFactory
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.NumberSpinner
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.editor.uibase.TableEditor
import rpgboss.model.ArmAdd
import rpgboss.model.AtkAdd
import rpgboss.model.Effect
import rpgboss.model.EffectContext
import rpgboss.model.MagAdd
import rpgboss.model.MetaEffect
import rpgboss.model.MhpAdd
import rpgboss.model.MmpAdd
import rpgboss.model.MreAdd
import rpgboss.model.RecoverHpAdd
import rpgboss.model.SpdAdd

class EffectPanel(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: Array[Effect],
  onUpdate: Array[Effect] => Unit,
  private var context: EffectContext.Value)
  extends BoxPanel(Orientation.Vertical) {

  def includeStatEffects =
    context != EffectContext.Skill && context != EffectContext.Enemy

  def updateContext(newContext: EffectContext.Value) = {
    context = newContext

    statEffectsPanel.enabled = includeStatEffects
  }

  if (includeStatEffects)
    preferredSize = new Dimension(300, 300)
  else
    preferredSize = new Dimension(250, 200)

  def isStatEffect(e: Effect) = {
    val statKeys =
      Set(MhpAdd, MmpAdd, AtkAdd, SpdAdd, MagAdd, ArmAdd, MreAdd).map(_.id)
    statKeys.contains(e.keyId)
  }

  def updateFromModel() = {
    onUpdate((statEffects ++ miscEffects).toArray)
  }

  val statEffects: collection.mutable.Set[Effect] =
    collection.mutable.Set(initial.filter(isStatEffect): _*)
  val statEffectsPanel = new DesignGridPanel {
    def statSpinner(metaEffect: MetaEffect) = {
      def spinFunc(newValue: Int) = {
        statEffects.retain(_.keyId != metaEffect.id)
        if (newValue != 0)
          statEffects.add(Effect(metaEffect.id, newValue))

        updateFromModel()
      }

      val initialValue =
        statEffects.find(metaEffect.matches _).map(_.v1).getOrElse(0)

      new NumberSpinner(initialValue, -999, 999, spinFunc)
    }

    row()
      .grid(lbl("+Max HP")).add(statSpinner(MhpAdd))
      .grid(lbl("+Attack")).add(statSpinner(AtkAdd))
    row()
      .grid(lbl("+Max MP")).add(statSpinner(MmpAdd))
      .grid(lbl("+Speed")).add(statSpinner(SpdAdd))

    row()
      .grid(lbl("+Armor")).add(statSpinner(ArmAdd))
      .grid(lbl("+Magic")).add(statSpinner(MagAdd))

    row()
      .grid(lbl("+Mag. Res.")).add(statSpinner(MreAdd))
      .grid()
  }

  val miscEffects = ArrayBuffer(initial.filter(!isStatEffect(_)): _*)
  val miscEffectsTable = new TableEditor[Effect]() {
    def title = "Other Effects"

    def modelArray = miscEffects
    def newInstance() = Effect(RecoverHpAdd.id)
    def onUpdate() = updateFromModel()

    def colHeaders = Array("Effect", "Value")
    def getRowStrings(effect: Effect) = {
      val meta = effect.meta
      Array(meta.name, meta.renderer(dbDiag.model, effect))
    }

    def showEditDialog(initial: Effect, okCallback: Effect => Unit) = {
      val diag = new EffectDialog(
        owner,
        dbDiag,
        initial,
        okCallback,
        context)
      diag.open()
    }
  }

  if (includeStatEffects) {
    contents += new BoxPanel(Orientation.Vertical) {
      border = BorderFactory.createTitledBorder("Stat boosts")
      contents += statEffectsPanel
    }
  }

  contents += miscEffectsTable
}