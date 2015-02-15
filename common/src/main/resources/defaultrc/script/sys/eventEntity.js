function FollowPlayer (eventid) {
	while(true) {

		var playerX = game.getPlayerX(),
				playerY = game.getPlayerY(),
				eventX = game.getEventX(eventid),
				eventY = game.getEventY(eventid);

		// TODO: Realize a wall is infront of the event
		// TODO: If event state changes kill this loop and restart it again if back to the state

		if(eventX < playerX) {
			game.moveEvent(eventid, 1, 0, false, false);
		} else if(eventY < playerY) {
			game.moveEvent(eventid, 0, 1, false, false);
		} else if(eventX > playerX) {
			game.moveEvent(eventid, -1, 0, false, false);
		} else if(eventY > playerY) {
			game.moveEvent(eventid, 0, -1, false, false);
		}

		game.sleep(0.3);
	}
}