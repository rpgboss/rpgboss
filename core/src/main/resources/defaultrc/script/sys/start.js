includeFile("sys/menu.js");

function newGame() {
  game.setTransition(1, 0.4);
  game.sleep(0.4);
  game.startNewGame();
}

function showStartDialog() {
  while (true) {
    var choiceWin = game.newChoiceWindow(
        [ game.getMessage("New Game"), 
          game.getMessage("Load Game"), 
          game.getMessage("Quit") ],
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

function splashScreen() {
//  game.setTransition(0, 1.0);
//  game.showPicture(0, 'sys/splash.jpg', 
//      game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0));
//  game.sleep(2);
//  game.setTransition(1, 0.4);
//  game.sleep(1);
}

function start() {
  splashScreen();
  game.playMusic(0, project.data().startup().titleMusic(), true, 0.4);
  game.showPicture(0, project.data().startup().titlePic(), 
      game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0));
  game.setTransition(0, 1.0);
  game.sleep(1);

  showStartDialog();
}
