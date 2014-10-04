function newGame() {
  game.setTransition(0, 1, 0.4);
  game.sleep(0.4);
  game.startNewGame();
}

function showStartDialog() {
  while (true) {
    var choiceWin = game.newChoiceWindow([ "New Game", "Load Game", "Quit" ],
        layout.centered(200, 130), {
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
          game.setTransition(0, 1, 0.4);
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
  game.setTransition(1, 0, 1.0);
  game.playMusic(0, project.data().startup().titleMusic(), true, 2.0);
  game.showPicture(0, project.data().startup().titlePic(), layout
      .centered(sizer.fit(640, 480)));

  showStartDialog();
}

function gameOver() {
}
