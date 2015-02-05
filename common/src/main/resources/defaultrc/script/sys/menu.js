function itemsMenu() {
  function enterItemsWindow(itemsTopWin, itemsMenu) {
    itemsMenu.window.takeFocus();
    itemsMenu.loopChoice(function(choiceId) {
      if (itemsMenu.state.itemIds.length == 0) {
        return true;
      }

      var itemId = itemsMenu.state.itemIds[choiceId];
      var usable = itemsMenu.state.itemUsabilities[choiceId];
      var itemsLeft = itemsMenu.state.itemQtys[choiceId];

      if (!usable || itemsLeft == 0) {
        return true;
      }

      var statusMenu = new StatusMenu();
      statusMenu.loopCharacterChoice(function onSelect(characterId) {
        if (itemsLeft > 0) {
          game.useItemInMenu(itemId, characterId);
        }

        --itemsLeft;

        // Don't return until after the user has had a chance to see the effect
        // of using the last item.
        return itemsLeft >= -1;
      });
      statusMenu.close();
      return true;
    });

    itemsTopWin.window.takeFocus();
  }

  var itemsMenu = new ItemMenu(
      true, game.layout(game.SOUTHWEST(), game.SCREEN(), 1.0, 0.87), 32);
  var itemsTopWin = new Menu({
    getState : function() {
      return {
        lines : [ "Use", "Organize" ]
      };
    },
    layout : game.layout(game.NORTHWEST(), game.SCREEN(), 1.0, 0.13),
    windowDetails : {
      justification : game.CENTER(),
      columns : 2,
      allowCancel : true
    }
  });

  itemsTopWin.loopChoice(function(choiceId) {
    if (choiceId == 0)
      enterItemsWindow(itemsTopWin, itemsMenu);
    return true;
  });

  itemsMenu.close();
  itemsTopWin.close();
}

function equipMenu(statusMenu) {
  statusMenu.window.takeFocus();

  function StatsMenu(characterId) {
    return new Menu({
      getState : function(state) {
        var stats = game.getBattleStats(characterId, -1, -1);
        var lines = [];
        
        lines.push(rightPad("Max HP:", 10) + leftPad(stats.current().mhp(), 4));
        lines.push(rightPad("Max MP:", 10) + leftPad(stats.current().mmp(), 4));
        lines.push(rightPad("ATK:", 10) + leftPad(stats.current().atk(), 4));
        lines.push(rightPad("SPD:", 10) + leftPad(stats.current().spd(), 4));
        lines.push(rightPad("MAG:", 10) + leftPad(stats.current().mag(), 4));
        lines.push(rightPad("ARM:", 10) + leftPad(stats.current().arm(), 4));
        lines.push(rightPad("MRE:", 10) + leftPad(stats.current().mre(), 4));

        return {
          lines : lines
        }
      },
      layout : game.layout(game.SOUTHWEST(), game.SCREEN(), 0.5, 0.6),
      windowDetails : {}
    });
  }

  function EquipMenu(characterId) {
    return new Menu({
      getState : function() {
        var equipment = game.getIntArray(game.CHARACTER_EQUIP(characterId));
        var slots = [ "Weapon", "Offhand", "Armor", "Accessory", "Accessory" ];
        var lines = [];

        var items = project.data().enums().items();
        
        for (var i = 0; i < slots.length; ++i) {
          var itemsString = "";
          if (i < equipment.length && equipment[i] >= 0) {
            itemsString = items[equipment[i]].name();
          }
          
          lines.push(rightPad(slots[i], 12) + itemsString);
        }
        
        return {
          lines: lines
        }
      },
      layout : game.layout(game.NORTH(), game.SCREEN(), 1.0, 0.4),
      windowDetails : {
        allowCancel: true
      }
    });
  }

  statusMenu.loopCharacterChoice(function(characterId) {
    var statsMenu = new StatsMenu(characterId);
    var equipMenu = new EquipMenu(characterId);
    
    var slotEquipTypes = [0, 1, 2, 4, 4];
    
    equipMenu.loopChoice(function(choiceId) {
      if (choiceId == -1)
        return false;
      
      var slotId = choiceId;
      var equipTypeId = slotEquipTypes[slotId];
      var itemsMenu = new Menu({
        getState : function() {
          var items = project.data().enums().items();
          var itemIds = game.getEquippableItems(characterId, equipTypeId);
          var itemNames = 
            itemIds.map(function(itemId) { return items[itemId].name(); });
          
          return {
            itemIds: itemIds,
            lines: itemNames
          }
        },
        layout : game.layout(game.SOUTHEAST(), game.SCREEN(), 0.5, 0.6),
        windowDetails : {
          allowCancel : true
        }
      });
      
      itemsMenu.loopChoice(function(choiceId) {
        if (itemsMenu.state.itemIds.length == 0)
          return false;
        
        var itemId = itemsMenu.state.itemIds[choiceId];
        game.equipItem(characterId, slotId, itemId);
        
        statsMenu.update();
        
        return false;
      })
      itemsMenu.close();
      
      return true;
    });
    
    statsMenu.close();
    equipMenu.close();
    return false;
  });
}

function menu() {
  var statusMenu = new StatusMenu();
  var rootMenuWin = new Menu({
    getState : function() {
      return {
        lines : [ "Item", "Skills", "Equip", "Status", "Save" ],
      };
    },
    layout : game.layout(game.NORTHEAST(), game.SCREEN(), 0.2, 0.8),
    windowDetails : {
      justification : game.CENTER(),
      allowCancel : true
    }
  });

  rootMenuWin.loopChoice(function(choiceId) {
    switch (choiceId) {
    case 0:
      itemsMenu();
      break;
    case 2:
      equipMenu(statusMenu);
      break;
    case 4:
      var saveMenu = new SaveAndLoadMenu();
      saveMenu.loopChoice(function(choiceId) {
        game.saveToSaveSlot(choiceId);
        return true;
      });
      saveMenu.close();
      break;
    }

    statusMenu.update();
    rootMenuWin.window.takeFocus();
    return true;
  });

  rootMenuWin.close();
  statusMenu.close();
}