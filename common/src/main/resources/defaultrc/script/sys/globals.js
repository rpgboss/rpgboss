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
  game.runScript("sys/menu.js", "SaveMenu()");
};

game.callMenu = function() {
  game.runScript("sys/menu.js", "menu()");
};

game.enterVehicle = function(vehicleId) {
  assert(vehicleId >= 0);
  assert(vehicleId < Constants.NUM_VEHICLES());
  
  var currentMapName = game.getMapName();
  var vehicleLoc = game.getLoc(game.VEHICLE_LOC(vehicleId));
  var playerInfo = game.getPlayerInfo();
  
  var dx = vehicleLoc.x() - playerInfo.x;
  var dy = vehicleLoc.y() - playerInfo.y;
  
  // If the vehicle is close, move into it. Otherwise, just teleport the vehicle
  // onto the player.
  if (currentMapName == vehicleLoc.map() && Math.abs(dx) + Math.abs(dy) < 10) {
    game.setPlayerCollision(false);
    game.movePlayer(dx, dy, false, false);
    game.setPlayerCollision(true);
  } else {
    game.placeVehicle(vehicleId, currentMapName, playerInfo.x, playerInfo.y);
  }
  
  game.setPlayerInVehicle(true, vehicleId);
};

game.getPlayerInfo = function() {
  var evt = game.getPlayerEntityInfo();
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
  var evt = game.getEventEntityInfo(id);
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

game.getStringInput = function(message, maxLength, initial) {
  var messageWindow = game.newTextWindow(
    [message], 
    game.layout(game.NORTHWEST(), game.SCALE_SOURCE(), 1, 1),
    {timePerChar: 0, showArrow: false, linesPerBlock: 1, justification: 1});
  
  var value = initial;
  if (value.length > maxLength)
    value = value.substring(0, maxLength);
  
  var valueWindow = game.newTextWindow(
    [value],
    game.layoutWithOffset(game.SOUTHWEST(), game.SCREEN(), 0.5, 0.15, 0.0, -0.6),
    {timePerChar: 0, showArrow: false, linesPerBlock: 1});

  // Convert to JS String, as that has different split semantics.
  var characters = String(project.data().startup().stringInputCharacters());
  var charArray = characters.split('');
  
  var choiceWindow = game.newChoiceWindow(
    charArray.concat(["ENTER"]),
    game.layout(game.SOUTH(), game.SCREEN(), 1.0, 0.6),
    {columns: 10});
  
  while (true) {
    var choiceIdx = choiceWindow.getChoice();

    if (choiceIdx == -1) {
      value = value.substring(0, value.length - 1);
    } else if (choiceIdx == charArray.length) {
      break;
    } else if (value.length < maxLength) {
      value = value + charArray[choiceIdx];
    }
    
    valueWindow.updateLines([value]);
  }
  
  choiceWindow.close();
  valueWindow.close();
  messageWindow.close();
  
  game.log("game.getStringInput result = " + value)
  return value;
};

game.setCharacterName = function(characterId, newName) {
  game.setStringArrayElement(game.CHARACTER_NAMES(), characterId, newName);
};

game.getCharacterNameFromPlayerInput = function(characterId) {
  var oldName = String(game.getCharacterName(characterId));
  var message = game.getMessage("Enter new name for: ") + oldName;
  
  var characterWindow = game.newTextWindow(
      [],
      game.layoutWithOffset(
          game.NORTHEAST(), game.FIXED(), 128 + 24*2, 128 + 24 * 2, 0, 0),
      {showArrow: false, useCharacterFace: true, characterId: characterId});
  
  var maxCharacterNameInputLength = 10;
  var newName = 
    game.getStringInput(message, maxCharacterNameInputLength, oldName);
  
  characterWindow.close();
    
  return newName;
};

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

/* This function can move and/or zoom a picture on the screen */
game.transformPicture = function(slot,
								name,
								widthStart,
								heightStart, 
								widthEnd, 
								heightEnd, 
								xOffsetStart, 
								yOffsetStart, 
								xOffsetEnd, 
								yOffsetEnd,  
								frames, 
								returnToStart, 
								keepPicture) {
	
	sizeType = 2
	startPosition = 8
	alpha = 1.0
	
	widthChange = ((widthStart-widthEnd) / frames)*-1;
	heightChange = ((heightStart-heightEnd) / frames)*-1;
	xOffsetChange = ((xOffsetStart-xOffsetEnd) / frames)*-1;
	yOffsetChange = ((yOffsetStart-yOffsetEnd) / frames)*-1;

	
	for(var i = 0; i < frames; i++) {
		
		game.showPicture(slot,
						 name, 
						 game.layoutWithOffset(startPosition, 
											   sizeType, 
											   (widthStart+(widthChange*i)), 
											   (heightStart+(heightChange*i)), 
											   (xOffsetStart+(xOffsetChange*i)), 
											   (yOffsetStart+(yOffsetChange*i))), 
						alpha);
		game.sleep(0.03);
	};
	
	if(returnToStart == Boolean(1)) {
	
		widthChange = ((widthStart-widthEnd) / frames);
		heightChange = ((heightStart-heightEnd) / frames);
		xOffsetChange = ((xOffsetStart-xOffsetEnd) / frames);
		yOffsetChange = ((yOffsetStart-yOffsetEnd) / frames);

	
		for(var i = 0; i < frames; i++) {
		
			game.showPicture(slot,
							name, 
							game.layoutWithOffset(startPosition, 
												sizeType, 
												(widthEnd+(widthChange*i)), 
												(heightEnd+(heightChange*i)), 
												(xOffsetEnd+(xOffsetChange*i)), 
												(yOffsetEnd+(yOffsetChange*i))), 
							alpha);
			game.sleep(0.03);
		};
	
	};
	if(keepPicture == Boolean(0)) {
		
		game.hidePicture(slot);
	};
};
