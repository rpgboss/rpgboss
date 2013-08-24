var kRootMenuWidth = 150;
var kLeftPaneWidth = 640 - kRootMenuWidth;

function itemsMenu() {
  var kItemsTopBarHeight = 64;
  var kItemsMainPanelHeight = 480 - kItemsTopBarHeight;
  var kItemsDisplayedItems = 10;
  
  function generateItemsWinChoices() {
    var inventoryIdxs = game.getGlobalArray("inventoryIdxs");
    var inventoryQtys = game.getGlobalArray("inventoryQtys");
    
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
        false /* closeOnSelect */,
        true /* allowCancel */);
  }
  
  function organizeItems(itemsMainWin) {
  }
  
  function enterItemsWindow(itemsTopWin, itemsMainWin) {
    game.focusWindow(itemsMainWin.id());
    
    while (true) {
      choiceIdx = itemsTopWin.getChoice();
      
      if (choiceIdx == -1)
        break;
    }
    
    
  }

  val itemsMainWin = generateItemsWin();
  val itemsTopWin = game.newChoiceWindow(
      ["Use", "Organize"],
      0, 0, leftPaneWidth, kItemsTopBarHeight,
      game.CENTER(),
      2 /* columns */,
      0 /* displayedLines */,
      false /* closeOnSelect */,
      true /* allowCancel */);
  
  var choiceIdx = 0;
  
  while (true) {
    switch (choiceIdx) {
      case 0:
        enterItemsWindow(itemsTopWin, itemsMainWin);
      case 1:
        organizeItems(itemsMainWin);
    }
    
    choiceIdx = itemsTopWin.getChoice();
    
    if (choiceIdx == -1)
      break;
  }
}

function menu() {
  var mainMenuWin = game.newChoiceWindow(
      ["Item", "Skills", "Equip", "Status", "Save"],
      640-kRootMenuWidth, 0, kRootMenuWidth, 480,
      game.CENTER(),
      1 /* columns */,
      0 /* displayedLines */,
      false /* closeOnSelect */,
      true /* allowCancel */);
  
  while (true) {
    var choiceIdx = mainMenuWin.getChoice();
    
    switch (choiceIdx) {
    case 0:
      itemsMenu();
      break;
    }
    
    if (choiceIdx == -1)
      break;
  }
  
  mainMenuWin.awaitClose();
  game.destroyWindow(mainMenuWin.id());
}
