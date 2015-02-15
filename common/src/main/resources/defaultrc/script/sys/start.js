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

/// GAME OVER

function showGameOverDialog() {
  while (true) {
    var choiceWin = game.newChoiceWindow([ "Back to titlescreen", "Quit game" ],
        game.layout(game.CENTERED(), game.FIXED(), 300, 100), {
          justification : game.CENTER()
        });

    var choiceIdx = choiceWin.getChoice();
    choiceWin.close();

    if (choiceIdx == 0) {
      game.setTransition(1, 0.4);
      game.sleep(0.4);
      // TODO: It doenst go to the titlescreen
      game.toTitleScreen();
      break;
    } else if (choiceIdx == 1) {
      game.quit();
      break;
    }
  }
}

function gameOver() {
  game.setTransition(0, 1.0);
  game.playMusic(0, project.data().startup().gameOverMusic(), true, 0.4);
  // TODO: GameOver picture will not be shown
  game.showPicture(30, project.data().startup().gameOverPic(), 
      game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0));
  game.sleep(0.1);
  showGameOverDialog();
}
