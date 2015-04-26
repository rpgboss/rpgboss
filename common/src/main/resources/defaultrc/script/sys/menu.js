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

      for (var i = 0; i < party.length; ++i) {
        var characterId = party[i];
        var stats = game.getBattleStats(characterId, -1, -1);

        lines.push(characterNames[characterId]);
        lines.push(" " + game.getMessage("LVL") + " "
            + leftPad(characterLevels[characterId].toString(), 3));
        lines.push("  " + game.getMessage("HP") + " "
            + leftPad(characterHps[characterId].toString(), 5) + " / "
            + leftPad(stats.current().mhp(), 5));
        lines.push("  " + game.getMessage("MP") + " "
            + leftPad(characterMps[characterId].toString(), 5) + " / "
            + leftPad(stats.current().mmp(), 5));
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
      lineHeight : 27,
      leftMargin : 96 + 24
    }
  }

  var menu = new Menu(details);

  // Attach party faces.
  var party = game.getIntArray(game.PARTY());
  for (var i = 0; i < party.length; ++i) {
    menu.window.attachCharacterFace(party[i], 0, (96 + 12) * i, 96);
  }

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
        lines.push(game.getMessage("Save") + " " + leftPad(i + 1, 2));
        var saveInfo = saveInfos[i];
        if (saveInfo.isDefined()) {
          lines.push(saveInfo.mapTitle());
        } else {
          lines.push("<" + game.getMessage("Empty") + ">");
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

function SaveMenu() {

  var saveMenu = new SaveAndLoadMenu();
  saveMenu.loopChoice(function(choiceId) {
    game.saveToSaveSlot(choiceId);
    saveMenu.close();
    return true;
  });
  saveMenu.close();
}

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

  var itemsMenu = new ItemMenu(true, game.layout(game.SOUTHWEST(), game
      .SCREEN(), 1.0, 0.87), 32);
  var itemsTopWin = new Menu({
    getState : function() {
      return {
        lines : [ game.getMessage("Use"), game.getMessage("Organize") ]
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

        lines.push(rightPad(game.getMessage("Max HP:"), 10)
            + leftPad(stats.current().mhp(), 5));
        lines.push(rightPad(game.getMessage("Max MP:"), 10)
            + leftPad(stats.current().mmp(), 5));
        lines.push(rightPad(game.getMessage("ATK:"), 10)
            + leftPad(stats.current().atk(), 5));
        lines.push(rightPad(game.getMessage("SPD:"), 10)
            + leftPad(stats.current().spd(), 5));
        lines.push(rightPad(game.getMessage("MAG:"), 10)
            + leftPad(stats.current().mag(), 5));
        lines.push(rightPad(game.getMessage("ARM:"), 10)
            + leftPad(stats.current().arm(), 5));
        lines.push(rightPad(game.getMessage("MRE:"), 10)
            + leftPad(stats.current().mre(), 5));

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
        var slots = [ game.getMessage("Weapon"), game.getMessage("Offhand"),
            game.getMessage("Armor"), game.getMessage("Accessory"),
            game.getMessage("Accessory") ];
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
          lines : lines
        }
      },
      layout : game.layout(game.NORTH(), game.SCREEN(), 1.0, 0.4),
      windowDetails : {
        allowCancel : true
      }
    });
  }

  statusMenu.loopCharacterChoice(function(characterId) {
    var statsMenu = new StatsMenu(characterId);
    var equipMenu = new EquipMenu(characterId);

    var slotEquipTypes = [ 0, 1, 2, 4, 4 ];

    equipMenu.loopChoice(function(choiceId) {
      if (choiceId == -1)
        return false;

      var slotId = choiceId;
      var equipTypeId = slotEquipTypes[slotId];
      var itemsMenu = new Menu({
        getState : function() {
          var items = project.data().enums().items();
          var itemIds = game.getEquippableItems(characterId, equipTypeId);
          var itemNames = itemIds.map(function(itemId) {
            return items[itemId].name();
          });

          return {
            itemIds : itemIds,
            lines : itemNames
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

function skillsMenu(statusMenu) {
  statusMenu.window.takeFocus();

  function SkillsMenu(characterId) {
    return new Menu({
      getState : function() {
        var mpLeft = 
          game.getIntArray(game.CHARACTER_MPS())[characterId];
        var knownSkillIds = game.getKnownSkills(characterId);
        var usableBools = [];
        var lines = [];
        var skills = [];
        
        var allSkills = project.data().enums().skills();
        
        for (var i = 0; i < knownSkillIds.length; ++i) {
          var skillId = knownSkillIds[i];
          var skill = allSkills[skillId];
          var usable = skill.cost() <= mpLeft;
          
          skills.push(skill);
          usableBools.push(usable);

          var lineParts = [];
          if (!usable)
            lineParts.push("\\c[1]");
          lineParts.push(skill.name());
          if (!usable)
            lineParts.push("\\c[0]");

          lines.push(lineParts.join(""));
        }

        return {
          mpLeft: mpLeft,
          skillIds: knownSkillIds,
          skills: skills,
          lines : lines,
          usableBools: usableBools
        }
      },
      layout : game.layout(game.NORTH(), game.SCREEN(), 1.0, 0.4),
      windowDetails : {
        allowCancel : true
      }
    });
  }

  statusMenu.loopCharacterChoice(function(characterId) {
    var skillsMenu = new SkillsMenu(characterId);

    skillsMenu.loopChoice(function(choiceId) {
      if (choiceId == -1)
        return false;

      var usable = skillsMenu.state.usableBools[choiceId];
      var skillId = skillsMenu.state.skillIds[choiceId];
      var skill = skillsMenu.state.skills[choiceId];

      if (!usable)
        return true;

      var usagesLeft = skill.cost() > 0 ? 
          Math.floor(skillsMenu.state.mpLeft / skill.cost()) : 1000;

      var statusMenu = new StatusMenu();
      statusMenu.loopCharacterChoice(function onSelect(targetCharacterId) {
        if (usagesLeft > 0) {
          game.useSkillInMenu(characterId, skillId, targetCharacterId);
        }

        --usagesLeft;

        // Don't return until after the user has had a chance to see the effect
        // of using the last skill.
        return usagesLeft >= -1;
      });
      statusMenu.close();
      
      return true;
    });

    skillsMenu.close();
    return false;
  });
}

function menu() {
  if (!game.getMenuEnabled())
    return;

  var statusMenu = new StatusMenu();
  var rootMenuWin = new Menu({
    getState : function() {
      return {
        lines : [ game.getMessage("Item"), game.getMessage("Skills"),
            game.getMessage("Equip"),
            game.getMessage("Save"), game.getMessage("Quit") ],
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
    case 1:
      skillsMenu(statusMenu);
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
    case 5:
      game.quit();
      break;
    }

    statusMenu.update();
    rootMenuWin.window.takeFocus();
    return true;
  });

  rootMenuWin.close();
  statusMenu.close();
}