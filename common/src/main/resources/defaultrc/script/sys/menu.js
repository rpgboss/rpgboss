function getItemChoices() {
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
      if (!usable)
        lineParts.push("\\c[1]");
      lineParts.push(rightPad(item.name(), 32));
      lineParts.push(" : " + itemQty.toString());
      if (!usable)
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
}

function newInventoryMenu() {
  var kItemsDisplayedItems = 10;

  var window = game.newChoiceWindow([],
      layout.southwest(sizer.prop(1.0, 0.87)), {
        displayedItems : kItemsDisplayedItems,
        allowCancel : true
      });

  var object = {
    window : window,
    update : updateObject
  }

  function updateObject() {
    var itemChoices = getItemChoices();
    window.updateLines(itemChoices.lines);
    object.itemIds = itemChoices.itemIds;
    object.itemQtys = itemChoices.itemQtys;
    object.itemUsabilities = itemChoices.itemUsabilities;
  }

  object.update();

  return object;
}

function enterItemsWindow(itemsTopWin, itemsMenu) {
  itemsMenu.window.takeFocus();

  while (true) {
    var choiceIdx = itemsMenu.window.getChoice();

    if (choiceIdx == -1)
      break;

    var itemId = itemsMenu.itemIds[choiceIdx];
    var usable = itemsMenu.itemUsabilities[choiceIdx];

    var itemsLeft = itemsMenu.itemQtys[choiceIdx];

    if (!usable || itemsLeft == 0)
      continue;

    loopPartyStatusChoice(function onSelect(characterId) {
      if (itemsLeft > 0) {
        game.useItemInMenu(itemId, characterId);
      }

      --itemsLeft;

      // Don't return until after the user has had a chance to see the effect
      // of using the last item.
      return itemsLeft >= -1;
    });

    itemsMenu.update();
  }

  itemsTopWin.takeFocus();
}

function itemsMenu() {
  var itemsMenu = newInventoryMenu();
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
      enterItemsWindow(itemsTopWin, itemsMenu);
      break;
    case 1:
      break;
    }

    choiceIdx = itemsTopWin.getChoice();

    if (choiceIdx == -1)
      break;
  }

  itemsMenu.window.close();
  itemsTopWin.close();
}

function menu() {
  var statusMenu = makeStatusWin();
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

    statusMenu.update();

    if (choiceIdx == -1)
      break;
  }

  rootMenuWin.close();
  statusMenu.window.close();
}