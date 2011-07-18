package rpgboss.rpgapplet.ui

import swing._

object RpgSimpleSwingApplication extends SimpleSwingApplication {
  def top = new MainFrame {
    contents = new TilesetEditorPanel
  }
}

// vim: set ts=4 sw=4 et:
