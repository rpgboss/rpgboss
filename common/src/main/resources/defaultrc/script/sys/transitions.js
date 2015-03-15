function transitionFade_Out(mapName, x, y, fadeDuration) {

  game.setTransition(0,fadeDuration);
  game.sleep(fadeDuration);

  game.setPlayerLoc(mapName,x,y);

  game.setTransition(1,fadeDuration);

}

function transitionNone(mapName, x, y, fadeDuration) {

  game.setPlayerLoc(mapName,x,y);
}

function transitionCustom1(mapName, x, y, fadeDuration) {}

function transitionCustom2(mapName, x, y, fadeDuration) {}

function transitionCustom3(mapName, x, y, fadeDuration) {}