includeFile('sys/weather/rain.js');
includeFile('sys/weather/fog.js');
includeFile('sys/weather/snow.js');

game.addScriptHook(function() {
	// Show effects if on
	ShowFog();
	ShowRain();
	ShowSnow();
	
	return true;
});