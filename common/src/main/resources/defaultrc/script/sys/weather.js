includeFile('sys/weather/rain.js');
includeFile('sys/weather/fog.js');
includeFile('sys/weather/snow.js');

// Update function
while(true) {

	ShowFog();
	ShowRain();
	ShowSnow();

	game.sleep(0.1);
}