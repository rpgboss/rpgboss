var snowStart = 180;
var snowCounter = snowStart;
var snowCounterMax = 192;
var snowSteps = 2;
var snowSwitch = false;

var snowImageWidth = 640;
var snowImageHeight = 480;
var soundCounter = 70;

function ShowSnow () {

	if(game.getInt("snowVisible")==1) {

		if(game.getInt("interior")==1) {

			game.hidePicture(54);

		} else {
			game.showPicture(54, "sys/weather/snow/0"+snowCounter+".png", game.layoutWithOffset(0,0,snowImageWidth,snowImageHeight,0,0),1);

			if(snowCounter>=snowCounterMax) {
				snowCounter = snowStart;
			}
			snowCounter++;
		}

	} else {
		game.hidePicture(54);
	}

}