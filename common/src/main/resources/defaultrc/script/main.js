function initCharacterStats(characters) {
  game.setIntArray(
    game.CHARACTER_LEVELS(), 
    characters.map(function(character) { return character.initLevel(); }));
  game.setIntArray(
    game.CHARACTER_HPS(),
    characters.map(function(character) { return character.initMhp(); }));
  game.setIntArray(
    game.CHARACTER_MPS(),
    characters.map(function(character) { return character.initMmp(); }));
  game.setIntArray(
    game.CHARACTER_MAX_HPS(),
    characters.map(function(character) { return character.initMhp(); }));
  game.setIntArray(
    game.CHARACTER_MAX_MPS(),
    characters.map(function(character) { return character.initMmp(); }));
}

function initializeData() {
/*
  // Initialize data structures
  game.setIntArray(game.PARTY(), project.data().startup().startingParty());
  
  var characters = project.data().enums().characters();
  game.setStringArray(game.CHARACTER_NAMES(), 
                      project.data().characterDefaultNames());
  initCharacterStats(characters);
*/
}

function main() {
  game.setTransition(0, 1, 600);
  game.playMusic(0, project.data().startup().titleMusic(), true, 2000);
  game.showPicture(0, project.data().startup().titlePic(), 0, 0, 640, 480);
  var winW = 200;
  var choiceWin = game.newChoiceWindow(
    ["New Game", "Load Game", "Quit"],
    320-winW/2, 280, winW, 130,
    game.CENTER(),
    1 /* columns */,
    0 /* displayedLines */,
    false /* allowCancel */);
  
  var choiceIdx = choiceWin.getChoice();
  choiceWin.destroy();
  
  game.setTransition(1, 0, 400);
  game.sleep(400);
  game.hidePicture(0);
  
  teleportLoc(project.data().startup().startingLoc(), Transitions.NONE().id());
  
  game.setPlayerLoc(project.data().startup().startingLoc());
  game.setTransition(0, 1, 400);
  
  initializeData();
}
