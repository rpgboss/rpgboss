var fogStart = 60;
var fogCounter = fogStart;
var fogCounterMax = 100;
var fogSteps = 2;
var fogSwitch = false;

var fogImageWidth = 1280;
var fogImageHeight = 960;

function ShowFog () {

	if(game.getInt("fogVisible")==1) {

		if(game.getInt("interior")==1) {

			game.hidePicture(52);
			
		} else {

			var multiplier = (fogCounter/fogCounterMax);

			var alpha = 1.0*multiplier;
			var offsetX = 50*multiplier;
			var offsetY = 50*multiplier;

			game.showPicture(52, "sys/weather/fog/fog.png", game.layoutWithOffset(0,0,fogImageWidth,fogImageHeight,offsetX,offsetY),alpha);

			if(fogSwitch) {
				fogCounter-=fogSteps;
			} else {
				fogCounter+=fogSteps;
			}
			if(fogCounter>fogCounterMax) {
				fogCounter = fogCounterMax;
				fogSwitch = true;
			}
			if(fogCounter<fogStart) {
				fogCounter = fogStart;
				fogSwitch = false;
			}

		}


	} {
		game.hidePicture(52);
	}

}