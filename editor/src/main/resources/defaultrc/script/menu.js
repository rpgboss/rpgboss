function menu() {

  var mainMenuW = 150;
  var mainMenuWin = game.newChoiceWindow(
      ["Item", "Skills", "Equip", "Status", "Save"],
      640-mainMenuW, 0, mainMenuW, 480,
      game.CENTER(),
      false /* closeOnSelect */,
      true /* allowCancel */);
  
  while (true) {
    var choiceIdx = mainMenuWin.getChoice();
    
    if (choiceIdx == -1)
      break;
  }
  
  mainMenuWin.awaitClose();
  game.destroyWindow(mainMenuWin.id());
}
