package rpgboss.editor.dialog
import scala.swing._
import scala.swing.event._
import rpgboss.editor.lib.DesignGridPanel
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.tileset.SpriteSelector
import com.weiglewilczek.slf4s.Logging

class SpriteSelectDialog(
    owner: Window, 
    project: Project,
    initialSelectionOpt: Option[SpriteSpec],
    onSuccess: (Option[SpriteSpec]) => Any)
  extends StdDialog(owner, "Select a Sprite") with Logging {
  
  val spritesets = Spriteset.list(project)
  
  var selection : Option[SpriteSpec] = None
  
  def okFunc(): Unit = { 
    onSuccess(selection)
    close()
  }
  
  val spritesetList = new ListView(spritesets)
  
  val spriteSelectorContainer = new BoxPanel(Orientation.Vertical) {
    def s = new Dimension(384, 384) // hardcoded to rpgmaker xp spriteset dim
    preferredSize = s
    maximumSize = s
    minimumSize = s
  }
  
  // Must call with valid arguments.
  // Call this only when updating both the spriteset and the spriteIndex
  def updateSelection(spriteSpecOpt: Option[SpriteSpec]) = {
    selection = spriteSpecOpt
    
    // Update the sprite index selection panel
    spriteSpecOpt.map { spriteSpec =>
      val spriteset = Spriteset.readFromDisk(project, spriteSpec.spriteset)
      
      spriteSelectorContainer.contents.clear()
      spriteSelectorContainer.contents += new SpriteSelector(
          spriteset,
          (spriteSpec: SpriteSpec) => {
            selection = Some(spriteSpec)
          }
      )
      
    } getOrElse {
      spriteSelectorContainer.contents.clear()
      spriteSelectorContainer.contents += new Label("No spriteset selected")
    }
    
    spriteSelectorContainer.revalidate()
  }
  
  // Initialize the selection, but first check to make sure it's valid
  updateSelection(initialSelectionOpt.flatMap { initSel =>
    if(spritesets.exists(initSel.spriteset == _)) {
      Some(initSel)
    } else {
      None
    }
  })

  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Spritesets"), leftLabel("Sprites"))
    row().grid().add(spritesetList, spriteSelectorContainer)
    addButtons(cancelButton, okButton)
  }
  
  val thiss = this
  
  listenTo(spritesetList.selection)
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
    case ListSelectionChanged(`spritesetList`, _, _) =>
      logger.info("Selected a different sprite")
      updateSelection(Some(SpriteSpec(spritesetList.selection.items.head, 0)))
  }
}