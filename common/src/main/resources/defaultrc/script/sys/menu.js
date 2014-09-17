function itemsMenu() {
  var kItemsDisplayedItems = 10;
	
  function generateItemsWinChoices() {
    var inventoryItemIds = game.getIntArray(game.INVENTORY_ITEM_IDS());
    var inventoryQtys = game.getIntArray(game.INVENTORY_QTYS());
    
    var choiceLines = [];
    var items = project.data().enums().items();
    for (var i = 0; 
         i < inventoryItemIds.length && i < inventoryQtys.length; 
         ++i) {
      if (inventoryItemIds < 0) {
        choiceLines.append("");
        continue;
      }
      
      // TODO: add real inv lines code
    }
    
    return choiceLines;
  }
  
  function generateItemsWin() {
    return game.newChoiceWindowWithOptions(
        generateItemsWinChoices(),
        layout.southwest(sizer.prop(1.0, 0.9)),
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
  var itemsTopWin = game.newChoiceWindowWithOptions(
      ["Use", "Organize"],
      layout.northwest(sizer.prop(1.0, 0.1)),
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
    lines.push(rightPad(characterNames[i], 10) + 
               leftPad(characters[i].subtitle(), 20));
    lines.push(" LVL " + leftPad(characterLevels[i].toString(), 3));
    lines.push("  HP " + leftPad(characterHps[i].toString(), 4) +
               " / " + leftPad(characterMaxHps[i].toString(), 4));
    lines.push("  MP " + leftPad(characterMps[i].toString(), 4) +
               " / " + leftPad(characterMaxMps[i].toString(), 4));
  }
  
  var statusWin = game.newChoiceWindow(
    lines,
    layout.northwest(sizer.prop(0.8, 1.0)));
  
  statusWin.setLineHeight(27);
  
  return statusWin;
}

function menu() {
  var statusWin = makeStatusWin();
  var rootMenuWin = game.newChoiceWindowWithOptions(
    ["Item", "Skills", "Equip", "Status", "Save"],
    layout.northeast(sizer.prop(0.2, 0.8)),
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
}
