function transitionFade_Out(mapName, x, y, fadeDuration) {

  game.setTransition(1,fadeDuration);
  game.sleep(fadeDuration);

  game.setPlayerLoc(mapName,x,y);

  game.setTransition(0,fadeDuration);

}

function transitionNone(mapName, x, y, fadeDuration) {

  game.setPlayerLoc(mapName,x,y);
}

function transitionCustom1(mapName, x, y, fadeDuration) {

  animator.AnimateSync(40, "transition/transition_Ebene-{number}.png", 
   game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0),21);

  game.setPlayerLoc(mapName,x,y);

  animator.ReverseAnimateSync(40, "transition/transition_Ebene-{number}.png", 
   game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0),21);
}

function transitionCustom2(mapName, x, y, fadeDuration) {}
function transitionCustom3(mapName, x, y, fadeDuration) {}