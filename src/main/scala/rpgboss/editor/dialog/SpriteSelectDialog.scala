package rpgboss.editor.dialog
import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.model._
import rpgboss.model.resource._

class SpriteSelectDialog(
    owner: Window, 
    project: Project,
    initialSelection: Option[SpriteSpec],
    onSuccess: (Option[SpriteSpec]) => Any)
  extends StdDialog(owner, "Select a Sprite") {
  
  var selection = initialSelection
  
  def okFunc(): Unit = { onSuccess(selection) }
  
  val spritesets = Spriteset.list(project)
  val spritesetList = new ListView(spritesets)
  var curSpriteset: Option[Spriteset] = None

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Spritesets"), leftLabel("Sprites"))
    addButtons(cancelButton, okButton)
  }
  
  val thiss = this
  
  reactions += {
    case WindowOpened(`thiss`) => 
      if(spritesets.isEmpty) {
        Dialog.showMessage(
          contents.head, // parent component is the panel. does this make sense?
          "No spritesets available.", 
          "Error",
          Dialog.Message.Error)
        close()
      }
  }
}