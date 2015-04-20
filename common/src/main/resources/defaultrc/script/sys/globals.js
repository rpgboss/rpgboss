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
  if (totalLen < castedString.length) {
    castedString = 
      'X' + castedString.substr(castedString.length - totalLen + 1);
  }
  var padLength = Math.max(totalLen - castedString.length, 0);
  return Array(padLength).join(" ") + castedString;
}

function rightPad(string, totalLen) {
  assert(typeof string != 'undefined');
  var castedString = String(string);
  if (totalLen < castedString.length) {
    castedString = castedString.substr(0, totalLen - 1) + 'X';
  }
  var padLength = Math.max(totalLen - castedString.length, 0);
  return castedString + Array(padLength).join(" ");
}

function zeroPad(number, totalLen) {
  var s = number + "";
  while (s.length < totalLen) s = "0" + s;
  return s.substr(s.length - totalLen);
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
  game.setWeather(0);
  game.setTransition(0, 1.0);
  game.playMusic(0, project.data().startup().gameOverMusic(), true, 0.4);
  game.showPicture(PictureSlots.GAME_OVER(), project.data().startup()
      .gameOverPic(), game.layout(game.CENTERED(), game.SCREEN(), 1.0, 1.0));
  game.sleep(0.1);
  
  while (true) {
    var choiceWin = game.newChoiceWindow([
        game.getMessage("Back to titlescreen"), game.getMessage("Quit game") ],
        game.layout(game.CENTERED(), game.FIXED(), 300, 100), {
          justification : game.CENTER()
        });
  
    var choiceIdx = choiceWin.getChoice();
    choiceWin.close();
  
    if (choiceIdx == 0) {
      game.setTransition(1, 0.4);
      game.sleep(0.4);
      game.toTitleScreen();
      break;
    } else if (choiceIdx == 1) {
      game.quit();
      break;
    }
  }
};

game.callSaveMenu = function() {
  game.runScript("sys/menu.js", "callSaveMenu()");
};

game.callMenu = function() {
  game.runScript("sys/menu.js", "callMenu()");
};

game.getPlayerInfo = function() {
  var evt = game.getPlayerInfoScala();
  return {
    x : evt.x(),
    y : evt.y(),
    dir : evt.dir(),
    screenX : evt.screenX(),
    screenY : evt.screenY(),
    screenTopLeftX : evt.screenTopLeftX(),
    screenTopLeftY : evt.screenTopLeftY(),
    width : evt.width(),
    height : evt.height()
  }
};

game.getEventInfo = function(id) {
  var evt = game.getEventInfoScala(id);
  return {
    x : evt.x(),
    y : evt.y(),
    dir : evt.dir(),
    screenX : evt.screenX(),
    screenY : evt.screenY(),
    screenTopLeftX : evt.screenTopLeftX(),
    screenTopLeftY : evt.screenTopLeftY(),
    width : evt.width(),
    height : evt.height()
  }
};

game.getMenuEnabled = function() {
  return game.getInt(game.MENU_ENABLED()) != 0;
};

/**
 * Returns -1 if user hits cancel.
 */
game.getNumberInput = function(message, digits, initial) {
  message = message || "Enter number:";
  digits = digits || 4;
  initial = initial || 0;
  
  var value = initial % Math.pow(10, digits);
  var curDigit = 0;

  var window = game.newTextWindow(
      [message, zeroPad(value, digits)],
      game.layout(game.CENTERED(), game.SCALE_SOURCE(), 1.0, 1.0),
      {timePerChar: 0, showArrow: false, linesPerBlock: 2, 
       justification: 1});

  var colorStart = "\\c[6]";
  var colorEnd = "\\c[0]";

  var done = false;
  while (!done) {
    var paddedValue = zeroPad(value, digits);
    var s = paddedValue;
    s =
      s.substring(0, s.length - curDigit - 1) +
      colorStart +
      s.charAt(s.length - curDigit - 1) +
      colorEnd +
      s.substring(s.length - curDigit);

    window.updateLines([message, s]);

    var key = game.getKeyInput([Keys.Up(), Keys.Down(), Keys.Left(),
                                Keys.Right(), Keys.OK(), Keys.Cancel()]);
    switch (key) {
    case 0:
      if (paddedValue.charAt(digits - curDigit - 1) == "9")
        value += -9 * Math.pow(10, curDigit);
      else
        value += Math.pow(10, curDigit);
      break;
    case 1:
      if (paddedValue.charAt(digits - curDigit - 1) == "0")
        value -= -9 * Math.pow(10, curDigit);
      else
        value -= Math.pow(10, curDigit);
      break;
    case 2:
      curDigit += 1;
      curDigit %= digits;
      break;
    case 3:
      curDigit -= 1;
      curDigit %= digits;
      break;
    case 4:
      done = true;
      break;
    default:
      done = true;
      value = -1;
    }
  }

  window.close();
  game.log("game.getNumberInput result = " + value.toString());

  return value;
}

game.setEventsEnabled = function(enabled) {
  game.setInt(game.EVENTS_ENABLED(), enabled ? 1 : 0);
};

game.setMenuEnabled = function(enabled) {
  game.setInt(game.MENU_ENABLED(), enabled ? 1 : 0);
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
        1.0 /* alpha */, 8 /* framesPerSecond */);
    game.stopMusic(MusicSlots.WEATHER(), 0.5 /* fadeDuration */);
    break;
  }
};

game.showText = function(text, options) {
  options = options || {};
  return game.showTextScala(text, options);
};