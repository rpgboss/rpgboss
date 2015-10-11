game.addScriptHook(function() {

	var timerValue = game.getInt('timer');
	if(timerValue!=0) {
		var minutes = Math.floor(timerValue/60),
				seconds = Math.floor(timerValue - (minutes*60));

		if(seconds==0) seconds = "00";
		if(minutes < 10) minutes = "0"+minutes;
		if(seconds < 10) seconds = "0"+seconds;

		var timeString = minutes + ':' + seconds;

		game.drawText(100, timeString , 20, 20, game.color(255,255,255,1),1);

		game.substractInt('timer', 1);
	} else {
		game.removeDrawedText(100);
	}
	
	return true;
});