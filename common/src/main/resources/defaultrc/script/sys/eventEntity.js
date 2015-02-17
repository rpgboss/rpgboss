var currentMap = game.getMapName(),
		eventstate = -1;

function FollowPlayer (eventid) {
	if(eventstate == -1) {
		eventstate = game.getEventState(game.getMapName(), eventid);
	}


	game.moveTowardsPlayer(eventid);

	game.sleep(0.1);

	if(game.getMapName() == currentMap
		&& eventstate == game.getEventState(game.getMapName(), eventid)) {
		FollowPlayer(eventid);
	}
}