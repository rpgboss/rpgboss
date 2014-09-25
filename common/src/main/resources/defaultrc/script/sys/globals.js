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

function getStatusLines() {
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

  return lines;
}

function makeStatusWin() {
  var lines = getStatusLines();

  var statusWin = game.newChoiceWindow(lines, layout.northwest(sizer.prop(0.8,
      1.0)), {
    allowCancel : true,
    linesPerChoice : 4,
    displayedLines : 8
  });

  statusWin.setLineHeight(27);

  return statusWin;
}

// onChoice is a callback that takes parameter (choiceId) and should
// return whether or not to select again.
// choiceId denotes the index inside the PARTY array, not the character id.
function getPartyChoice(onChoice) {
  var window = makeStatusWin();
  while (true) {
    var choiceIdx = window.getChoice();

    if (choiceIdx == -1)
      break;

    var shouldContinue = onChoice(choiceIdx);
    if (!shouldContinue)
      break;

    window.updateLines(getStatusLines());
  }
  window.close();
}