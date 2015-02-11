includeFile('sys/weather/rain.js');
includeFile('sys/weather/fog.js');

// Update function
while(true) {

	ShowFog();
	ShowRain();

	game.sleep(0.01);
}