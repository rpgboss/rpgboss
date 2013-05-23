function teleport(mapName, x, y, transition) {
	if (Transitions.get(transition) == Transitions.FADE()) {
		game.setTransition(1, 0, Transitions.fadeLength());
		game.sleep(Transitions.fadeLength());
	}

	var loc = MapLoc.apply(mapName, x, y)

	game.setPlayerLoc(loc);
	game.setCameraLoc(loc);

	if (Transitions.get(transition) == Transitions.FADE()) {
		game.setTransition(0, 1, Transitions.fadeLength());
	}
}