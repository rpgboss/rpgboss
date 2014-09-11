function newGame() {
  game.setTransition(0, 1, 0.4);
  game.sleep(0.4);
  game.hidePicture(0);
  
  var loc = project.data().startup().startingLoc();
  game.teleport(
    loc.map(), loc.x(), loc.y(), Transitions.NONE().id());
  
  game.startNewGame();
}

function showStartDialog() {
  while (true) {
    var choiceWin = game.newChoiceWindowWithOptions(
      ["New Game", "Load Game", "Quit"],
      layout.centered(200, 130),
      game.CENTER(),
      1 /* columns */,
      0 /* displayedLines */,
      false /* allowCancel */);
    
    var choiceIdx = choiceWin.getChoice();
    choiceWin.close();
    
    if (choiceIdx == 0) {
      newGame();
      break;
    } else if (choiceIdx == 2) {
      game.quit();
      break;
    }
  }
}

function start() {
  game.setTransition(1, 0, 0.4);
  game.playMusic(0, project.data().startup().titleMusic(), true, 2.0);
  game.showPicture(0, project.data().startup().titlePic(), 
    layout.centered(640, 480));
  
  showStartDialog();
}

function gameOver() {
}
