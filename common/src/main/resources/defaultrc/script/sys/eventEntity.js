function FollowPlayer (eventid) {

	game.moveTowardsPlayer(eventid);

	game.sleep(0.3);

	FollowPlayer(eventid);
}