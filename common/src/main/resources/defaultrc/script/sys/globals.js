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

function Menu(details) {
  assertDefined(details.getState);
  assertDefined(details.layout);
  assertDefined(details.windowDetails);

  this.updateState = function() {
    this.state = details.getState();
  }
  this.updateState();

  this.window = game.newChoiceWindow(this.state.lines, details.layout,
      details.windowDetails);

  this.update = function() {
    this.updateState();
    this.window.updateLines(this.state.lines);
  }

  this.close = function() {
    this.window.close();
  }

  // onChoice is a callback that takes choice index and should return whether
  // or not to select again.
  this.loopChoice = function(onChoice) {
    while (true) {
      var choiceIdx = this.window.getChoice();

      if (choiceIdx == -1)
        break;

      var shouldContinue = onChoice(choiceIdx);
      if (!shouldContinue)
        break;

      this.update();
    }
  }
}

function ItemMenu(displayUsability, menuLayout, rightPadding) {
  var menu = new Menu({
    getState : function() {
      var itemIds = game.getIntArray(game.INVENTORY_ITEM_IDS());
      var itemQtys = game.getIntArray(game.INVENTORY_QTYS());
      var itemUsabilities = [];

      var choiceLines = [];
      var items = project.data().enums().items();
      for (var i = 0; i < itemIds.length && i < itemQtys.length; ++i) {
        var itemId = itemIds[i];
        var itemQty = itemQtys[i];
        if (itemId < 0 || itemQty <= 0) {
          choiceLines.push("");
        } else {
          var item = items[itemId];
          var usable = item.usableInMenu();
          itemUsabilities.push(usable);

          var lineParts = [];
          if (displayUsability && !usable)
            lineParts.push("\\c[1]");
          lineParts.push(rightPad(item.name(), rightPadding));
          lineParts.push(" : " + itemQty.toString());
          if (displayUsability && !usable)
            lineParts.push("\\c[0]");

          choiceLines.push(lineParts.join(""));
        }
      }

      return {
        lines : choiceLines,
        itemIds : itemIds,
        itemQtys : itemQtys,
        itemUsabilities : itemUsabilities
      }
    },
    layout : menuLayout,
    windowDetails : {
      displayedItems : 10,
      allowCancel : true
    }
  });

  return menu;
}

function StatusMenu() {
  var details = {
    getState : function() {
      var lines = [];
      var party = game.getIntArray(game.PARTY());

      var characters = project.data().enums().characters();
      var characterNames = game.getStringArray(game.CHARACTER_NAMES());
      var characterLevels = game.getIntArray(game.CHARACTER_LEVELS());
      var characterHps = game.getIntArray(game.CHARACTER_HPS());
      var characterMps = game.getIntArray(game.CHARACTER_MPS());
      var characterMaxHps = game.getIntArray(game.CHARACTER_MAX_HPS());
      var characterMaxMps = game.getIntArray(game.CHARACTER_MAX_MPS());

      for (var i = 0; i < party.length; ++i) {
        lines.push(rightPad(characterNames[i], 10)
            + leftPad(characters[i].subtitle(), 20));
        lines.push(" LVL " + leftPad(characterLevels[i].toString(), 3));
        lines.push("  HP " + leftPad(characterHps[i].toString(), 4) + " / "
            + leftPad(characterMaxHps[i].toString(), 4));
        lines.push("  MP " + leftPad(characterMps[i].toString(), 4) + " / "
            + leftPad(characterMaxMps[i].toString(), 4));
      }

      return {
        lines : lines,
        party : party
      }
    },
    layout : game.layout(game.NORTHWEST(), game.SCREEN(), 0.8, 1.0),
    windowDetails : {
      allowCancel : true,
      linesPerChoice : 4,
      lineHeight : 27
    }
  }

  var menu = new Menu(details);
  menu.loopCharacterChoice = function(onCharacterChoice) {
    menu.loopChoice(function(choiceIdx) {
      return onCharacterChoice(menu.state.party[choiceIdx]);
    });
  }
  return menu
}

function SaveAndLoadMenu() {
  var kMaxSlots = 15;
  return new Menu({
    getState : function() {
      var saveInfos = game.getSaveInfos(kMaxSlots);
      var lines = [];

      for (var i = 0; i < saveInfos.length; ++i) {
        lines.push("Save " + leftPad(i + 1, 2));
        var saveInfo = saveInfos[i];
        if (saveInfo.isDefined()) {
          lines.push(saveInfo.mapTitle());
        } else {
          lines.push("<Empty>");
        }
        lines.push("");
      }

      return {
        lines : lines,
        saveInfos : saveInfos
      }
    },
    layout : game.layout(game.CENTERED(), game.FIXED(), 320, 320),
    windowDetails : {
      allowCancel : true,
      linesPerChoice : 3,
      displayedLines : 9
    }
  });
}