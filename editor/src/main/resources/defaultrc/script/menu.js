var kRootMenuWidth = 150;
var kLeftPaneWidth = 640 - kRootMenuWidth;

function itemsMenu() {
  var kItemsTopBarHeight = 64;
  var kItemsMainPanelHeight = 480 - kItemsTopBarHeight;
  var kItemsDisplayedItems = 10;
  
  function generateItemsWinChoices() {
    var inventoryIdxs = game.getIntArray(game.INVENTORY_IDXS());
    var inventoryQtys = game.getIntArray(game.INVENTORY_QTYS());
    
    var choiceLines = [];
    var items = project.data().enums().items();
    for (var i = 0; i < inventoryIdxs.length && i < inventoryQtys.length; ++i) {
      if (inventoryIdxs < 0) {
        choiceLines.append("");
        continue;
      }
      
      // TODO: add real inv lines code
    }
    
    return choiceLines;
  }
  
  function generateItemsWin() {
    return game.newChoiceWindow(
        generateItemsWinChoices(),
        0, kItemsTopBarHeight, kLeftPaneWidth, kItemsMainPanelHeight,
        game.LEFT(),
        1 /* columns */,
        kItemsDisplayedItems /* displayedLines */,
        true /* allowCancel */);
  }
  
  function organizeItems(itemsMainWin) {
  }
  
  function enterItemsWindow(itemsTopWin, itemsMainWin) {
    itemsMainWin.takeFocus();
    
    var choiceIdx;
    while (true) {
      choiceIdx = itemsMainWin.getChoice();
      
      if (choiceIdx == -1)
        break;
    }
    
    itemsTopWin.takeFocus();
  }

  var itemsMainWin = generateItemsWin();
  var itemsTopWin = game.newChoiceWindow(
      ["Use", "Organize"],
      0, 0, kLeftPaneWidth, kItemsTopBarHeight,
      game.CENTER(),
      2 /* columns */,
      0 /* displayedLines */,
      true /* allowCancel */);
  
  var choiceIdx = 0;
  
  while (true) {
    switch (choiceIdx) {
      case 0:
        enterItemsWindow(itemsTopWin, itemsMainWin);
        break;
      case 1:
        organizeItems(itemsMainWin);
        break;
    }
    
    choiceIdx = itemsTopWin.getChoice();
    
    if (choiceIdx == -1)
      break;
  }
  
  itemsMainWin.close();
  itemsTopWin.close();
  itemsMainWin.destroy();
  itemsTopWin.destroy();
}

function makeStatusWin() {
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
    lines.push(rightPad(characterNames[i], 10) + characters[i].subtitle());
    lines.push(" LVL " + leftPad(characterLevels[i].toString(), 4));
    lines.push("  HP " + leftPad(characterHps[i].toString(), 4) +
               " / " + leftPad(characterMaxHps[i].toString(), 4));
    lines.push("  MP " + leftPad(characterMps[i].toString(), 4) +
               " / " + leftPad(characterMaxMps[i].toString(), 4));
  }
  
  var statusWin = game.newChoiceWindow(
    lines,
    0, 0, kLeftPaneWidth, 480,
    game.LEFT(),
    1 /* columns */,
    0 /* displayedLines */,
    true /* allowCancel */);
  
  return statusWin;
}

function menu() {
  var statusWin = makeStatusWin();
  var rootMenuWin = game.newChoiceWindow(
    ["Item", "Skills", "Equip", "Status", "Save"],
    640-kRootMenuWidth, 0, kRootMenuWidth, 480,
    game.CENTER(),
    1 /* columns */,
    0 /* displayedLines */,
    true /* allowCancel */);
  
  while (true) {
    var choiceIdx = rootMenuWin.getChoice();
    
    switch (choiceIdx) {
    case 0:
      itemsMenu();
      break;
    }
    
    if (choiceIdx == -1)
      break;
  }

  rootMenuWin.close();
  statusWin.close();
  rootMenuWin.destroy();
  statusWin.destroy();
}
