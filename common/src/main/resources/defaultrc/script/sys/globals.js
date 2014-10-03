// Casting to support both Javascript and Java strings.
function leftPad(string, totalLen) {
  var castedString = String(string);
  var padLength = Math.max(totalLen - castedString.length, 0);
  return Array(padLength).join(" ") + castedString;
}

function rightPad(string, totalLen) {
  var castedString = String(string);
  var padLength = Math.max(totalLen - castedString.length, 0);
  return castedString + Array(padLength).join(" ");
}

function StatusMenu() {
  this.updateStatusLines = function() {
    this.lines = [];
    this.party = game.getIntArray(game.PARTY());

    var characters = project.data().enums().characters();
    var characterNames = game.getStringArray(game.CHARACTER_NAMES());
    var characterLevels = game.getIntArray(game.CHARACTER_LEVELS());
    var characterHps = game.getIntArray(game.CHARACTER_HPS());
    var characterMps = game.getIntArray(game.CHARACTER_MPS());
    var characterMaxHps = game.getIntArray(game.CHARACTER_MAX_HPS());
    var characterMaxMps = game.getIntArray(game.CHARACTER_MAX_MPS());

    for (var i = 0; i < this.party.length; ++i) {
      this.lines.push(rightPad(characterNames[i], 10)
          + leftPad(characters[i].subtitle(), 20));
      this.lines.push(" LVL " + leftPad(characterLevels[i].toString(), 3));
      this.lines.push("  HP " + leftPad(characterHps[i].toString(), 4) + " / "
          + leftPad(characterMaxHps[i].toString(), 4));
      this.lines.push("  MP " + leftPad(characterMps[i].toString(), 4) + " / "
          + leftPad(characterMaxMps[i].toString(), 4));
    }
  }

  this.updateStatusLines();

  this.window = game.newChoiceWindow(this.lines, layout.northwest(sizer.prop(
      0.8, 1.0)), {
    allowCancel : true,
    linesPerChoice : 4,
    lineHeight : 27
  });

  this.update = function() {
    this.updateStatusLines();
    this.window.updateLines(this.lines);
  }

  this.close = function() {
    this.window.close();
  }

  // onChoice is a callback that takes parameter (characterId) and should
  // return whether or not to select again.
  // choiceId denotes the character id, not the choice index.
  this.loopChoice = function(onChoice) {
    while (true) {
      var choiceIdx = this.window.getChoice();

      if (choiceIdx == -1)
        break;

      var shouldContinue = onChoice(this.party[choiceIdx]);
      if (!shouldContinue)
        break;

      this.update();
    }
    this.close();
  }
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