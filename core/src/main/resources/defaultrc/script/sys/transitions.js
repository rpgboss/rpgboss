// In all these methods, the setPlayerLoc must come last. This is because
// the thread is killed after that is called.
function transitionFade_Out(mapName, x, y, fadeDuration) {
  game.setTransition(1, fadeDuration);
  game.sleep(fadeDuration);

  game.setTransition(0, fadeDuration);
  game.setPlayerLoc(mapName, x, y);
}

// In all these methods, the setPlayerLoc must come last. This is because
// the thread is killed after that is called.
function transitionNone(mapName, x, y, fadeDuration) {
  game.setPlayerLoc(mapName, x, y);
}

// In all these methods, the setPlayerLoc must come last. This is because
// the thread is killed after that is called.
function transitionCustom1(mapName, x, y, fadeDuration) {
}

// In all these methods, the setPlayerLoc must come last. This is because
// the thread is killed after that is called.
function transitionCustom2(mapName, x, y, fadeDuration) {
}

// In all these methods, the setPlayerLoc must come last. This is because
// the thread is killed after that is called.
function transitionCustom3(mapName, x, y, fadeDuration) {
}
