function includeFile(scriptPath) {
  // Need to convert to JavaScript String, as eval does not play nice with 
  // java.lang.String.
  var scriptString = String(game.getScriptAsString(scriptPath));
  
  // Evaluate at the global scope. I hope users are only calling this function
  // at the global scope.
  return eval.call(this, scriptString);
}

// Casting to support both Javascript and Java strings.
function leftPad(string, totalLen) {
  assert(typeof string != 'undefined');
  var castedString = String(string);
  assert(totalLen >= castedString.length,
      "padding failed: totalLen = " + totalLen +
      " string.length = " + castedString.length);
  var padLength = Math.max(totalLen - castedString.length, 0);
  return Array(padLength).join(" ") + castedString;
}

function rightPad(string, totalLen) {
  assert(typeof string != 'undefined');
  var castedString = String(string);
  assert(totalLen >= castedString.length,
      "padding failed: totalLen = " + totalLen +
      " string.length = " + castedString.length);
  var padLength = Math.max(totalLen - castedString.length, 0);
  return castedString + Array(padLength).join(" ");
}

function assert(condition, message) {
  if (!condition) {
    message = message || "Assertion failed";
    if (typeof Error !== "undefined") {
      throw new Error(message);
    }
    throw message; // Fallback
  }
}

function assertDefined(variable) {
  assert(typeof variable !== 'undefined');
}

function getItemName(itemId) {
  var items = project.data().enums().items();
  if (itemId < items.length) {
    var item = items[itemId];
    return item.name();
  } else {
    return "ITEM_ID_OUT_OF_BOUNDS";
  }
}

var game = Object.create(scalaScriptInterface);

game.gameOver = function() {
  game.runScript("sys/menu.js", "gameOver()");
};

game.callSaveMenu = function() {
  game.runScript("sys/menu.js", "callSaveMenu()");
};

game.callMenu = function() {
  game.runScript("sys/menu.js", "callMenu()");
};

game.setWeather = function(weatherTypeId) {
  switch(weatherTypeId) {
  case 0:
    game.hidePicture(PictureSlots.WEATHER());
    game.stopMusic(MusicSlots.WEATHER(), 0.5 /* fadeDuration */);
    break;
  case 1:  // Rain
    game.showPictureLoop(
        PictureSlots.WEATHER(), 
        'sys/weather/rain', 
        game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0),
        1.0 /* alpha */, 14 /* framesPerSecond */);
    game.playMusic(
        MusicSlots.WEATHER(),
        'sys/weather/rain.mp3',
        0.5 /* volume */, true /* loop */, 0.5 /* fadeDuration */);
    break;
  case 2:
    game.showPictureLoop(
        PictureSlots.WEATHER(), 
        'sys/weather/snow', 
        game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0),
        1.0 /* alpha */, 30 /* framesPerSecond */);
    game.stopMusic(MusicSlots.WEATHER(), 0.5 /* fadeDuration */);
    break;
  }
}