var currentMap = game.getMapName();

function FollowPlayer (eventid) {

	game.moveTowardsPlayer(eventid);

	game.sleep(0.3);

	if(game.getMapName() == currentMap) {
		FollowPlayer(eventid);
	}
}