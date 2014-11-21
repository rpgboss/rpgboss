package rpgboss.editor.misc

import scala.collection.mutable.ArrayBuffer
import scala.swing.Window

import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.NumberSpinner
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.SwingUtils.indexedCombo
import rpgboss.editor.uibase.SwingUtils.lbl
import rpgboss.editor.uibase.TableEditor
import rpgboss.lib.Utils
import rpgboss.model.ProjectData
import rpgboss.model.RandomEncounter
import rpgboss.model._

class RandomEncounterListPanel(
    owner: Window,
    projectData: ProjectData,
    initial: Array[RandomEncounter],
    onUpdate: Array[RandomEncounter] => Unit)
    extends TableEditor[RandomEncounter] {
  override def title = "Random Encounters"

  override val modelArray = ArrayBuffer(initial: _*)
  override def newInstance() = RandomEncounter()

  override def onUpdate() = onUpdate(modelArray.toArray)

  override def colHeaders = Array("Encounter", "Weight", "% Chance")

  def totalWeights = modelArray.map(_.weight).sum

  override def getRowStrings(encounter: RandomEncounter) = {
    Array(
      projectData.enums.encounters(encounter.encounterId).name,
      encounter.weight.toString,
      Utils.floatToPercent(encounter.weight.toFloat / totalWeights))
  }

  override def showEditDialog(
      initial: RandomEncounter, okCallback: RandomEncounter => Unit) = {
    val d = new StdDialog(owner, "Random Encounter") {
      val model = initial.copy()
      def okFunc() = {
        okCallback(model)
        close()
      }

      val fEncounterId = indexedCombo(
          projectData.enums.encounters,
          model.encounterId,
          model.encounterId = _)

      val fWeight =
        new NumberSpinner(model.weight, 1, 100, model.weight = _)

      contents = new DesignGridPanel {
        row().grid(lbl("Encounter:")).add(fEncounterId)
        row().grid(lbl("Weight:")).add(fWeight)
        addButtons(okBtn, cancelBtn)
      }
    }
    d.open()
  }
}