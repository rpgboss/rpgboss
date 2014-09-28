function getItemChoiceLines() {
  var inventoryItemIds = game.getIntArray(game.INVENTORY_ITEM_IDS());
  var inventoryQtys = game.getIntArray(game.INVENTORY_QTYS());

  var choiceLines = [];
  var items = project.data().enums().items();
  for (var i = 0; i < inventoryItemIds.length && i < inventoryQtys.length; ++i) {
    var itemId = inventoryItemIds[i];
    var itemQty = inventoryQtys[i];
    if (itemId < 0 || itemQty <= 0) {
      choiceLines.push("");
    } else {
      var item = items[itemId];
      var usable = item.usableInMenu();

      var lineParts = [];
      if (!usable)
        lineParts.push("\\c[1]");
      lineParts.push(rightPad(item.name(), 32));
      lineParts.push(" : " + itemQty.toString());
      if (!usable)
        lineParts.push("\\c[0]");

      choiceLines.push(lineParts.join(""));
    }
  }

  return choiceLines;
}

function newInventoryMenu() {
  var kItemsDisplayedItems = 10;

  return game.newChoiceWindow(
      getItemChoiceLines(),
      layout.southwest(sizer.prop(1.0, 0.87)),
      {
        displayedItems : kItemsDisplayedItems,
        allowCancel : true
      });
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

function itemsMenu() {
  function organizeItems(itemsMainWin) {
  }

  var itemsMainWin = newInventoryMenu();
  var itemsTopWin = game.newChoiceWindow([ "Use", "Organize" ], layout
      .northwest(sizer.prop(1.0, 0.13)), {
    justification : game.CENTER(),
    columns : 2,
    allowCancel : true
  });

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

function menu() {
  var statusWin = makeStatusWin();
  var rootMenuWin = game.newChoiceWindow([ "Item", "Skills", "Equip", "Status",
      "Save" ], layout.northeast(sizer.prop(0.2, 0.8)), {
    justification : game.CENTER(),
    allowCancel : true
  });

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