var rainStart = 180;
var rainCounter = rainStart;
var rainCounterMax = 192;
var rainSteps = 2;
var rainSwitch = false;

var rainImageWidth = 640;
var rainImageHeight = 480;
var soundCounter = 70;

function ShowRain () {

	if(game.getInt("rainVisible")==1) {

		if(game.getInt("interior")==1) {

			game.hidePicture(53);

			if(soundCounter>=70) {
				game.playSound('sys/weather/rain.mp3',0.5,1);
				soundCounter=0;
			}
			soundCounter++;

		} else {
			game.showPictureLoop(53, 'sys/weather/rain', game.layoutWithOffset(0,0,rainImageWidth,rainImageHeight,0,0), 1,14);

			if(rainCounter>=rainCounterMax) {
				rainCounter = rainStart;
			}
			rainCounter++;

			if(soundCounter>=70) {
				game.playSound('sys/weather/rain.mp3');
				soundCounter=0;
			}
			soundCounter++;
		}

	} else {
		game.hidePicture(53);
	}

}