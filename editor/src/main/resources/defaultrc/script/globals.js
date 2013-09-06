function teleportLoc(loc, transition) {
  var map = game.getMap(loc);
  var fadeDuration = Transitions.fadeLength();
  
  if (map.metadata().changeMusicOnEnter()) {
    game.playMusic(0, map.metadata().music(), true, fadeDuration);
  }
  
  if (Transitions.get(transition) == Transitions.FADE()) {
    game.setTransition(1, 0, fadeDuration);
    game.sleep(fadeDuration);
  }

  game.setPlayerLoc(loc);

  if (Transitions.get(transition) == Transitions.FADE()) {
    game.setTransition(0, 1, fadeDuration);
  }
}

function teleport(mapName, x, y, transition) {
  return teleportLoc(MapLoc.apply(mapName, x, y), transition);
}

function leftPad(string, totalLen) {
  return Array(totalLen + 1 - string.length).join(" ") + string;
}

function rightPad(string, totalLen) {
  return string + Array(totalLen + 1 - string.length).join(" ");
}