function transition0(mapName, x, y, fadeDuration) {

  game.setTransition(1,fadeDuration);
  game.sleep(fadeDuration);

  game.setPlayerLoc(mapName,x,y);

  game.setTransition(0,fadeDuration);

}

function transition1(mapName, x, y, fadeDuration) {

  game.setPlayerLoc(mapName,x,y);
}

function newGame() {
  game.setTransition(1, 0.4);
  game.sleep(0.4);
  game.startNewGame();
}

function showStartDialog() {
  while (true) {
    var choiceWin = game.newChoiceWindow([ "New Game", "Load Game", "Quit" ],
        game.layout(game.CENTERED(), game.FIXED(), 200, 130), {
          justification : game.CENTER()
        });

    var choiceIdx = choiceWin.getChoice();
    choiceWin.close();

    if (choiceIdx == 0) {
      newGame();
      break;
    } else if (choiceIdx == 1) {
      var loadMenu = new SaveAndLoadMenu();

      loadMenu.loopChoice(function(choiceId) {
        if (loadMenu.state.saveInfos[choiceId].isDefined()) {
          game.setTransition(1, 0.4);
          game.sleep(0.4);
          game.loadFromSaveSlot(choiceId);
        }

        return false;
      });

      loadMenu.close();
    } else if (choiceIdx == 2) {
      game.quit();
      break;
    }
  }
}

function start() {
  game.setTransition(0, 1.0);
  game.playMusic(0, project.data().startup().titleMusic(), true, 0.4);
  game.showPicture(0, project.data().startup().titlePic(), 
      game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0));

  showStartDialog();
}

function gameOver() {
}
