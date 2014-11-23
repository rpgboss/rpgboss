function StoreBuyMenu(itemIds, priceMultiplier) {
  var menu = new Menu({
    getState : function() {
      var choiceLines = [];
      var itemPrices = [];
      var items = project.data().enums().items();
      for (var i = 0; i < itemIds.length; ++i) {
        var itemId = itemIds[i];
        var item = items[itemId];
        var price = item.price() * priceMultiplier;
        itemPrices.push(price);

        var lineParts = [];
        lineParts.push(rightPad(item.name(), 20));
        lineParts.push(leftPad(price.toString(), 5));
        
        choiceLines.push(lineParts.join(""));
      }

      return {
        lines : choiceLines,
        itemIds : itemIds,
        itemPrices: itemPrices
      }
    },
    layout : layout.southwest(sizer.prop(0.6, 0.87)),
    windowDetails : {
      displayedItems : 10,
      allowCancel : true
    }
  });

  return menu;
}

function openStore(itemIdsSold, buyPriceMultiplier, sellPriceMultiplier) {
  var storeHeaderWin = game.newStaticTextWindow(
    ["Store"],
    layout.northwest(sizer.prop(0.5, 0.13)),
    { justification : game.CENTER() });
  
  var storeRightPane = game.newStaticTextWindow(
    [], layout.southeast(sizer.prop(0.4, 0.87)), {})
    
  function updateStoreRightPane() {
    storeRightPane.updateLines(
        ["Gold: ",
         "  " + game.getInt(Constants.GOLD())])
  }
  updateStoreRightPane();
  
  var storeTopWin = new Menu({
    getState : function() {
      return {
        lines : ["Buy", "Sell"]
      };
    },
    layout : layout.northeast(sizer.prop(0.5, 0.13)),
    windowDetails : {
      justification : game.CENTER(),
      columns : 2,
      allowCancel : true
    }
  });

  storeTopWin.loopChoice(function(choiceId) {
    if (choiceId == 0) {
      var buyMenu = new StoreBuyMenu(itemIdsSold, buyPriceMultiplier);
      buyMenu.loopChoice(function(choiceId) {
        updateStoreRightPane();
        return true;
      });
      buyMenu.close();
    } else {
      
    }
    return true;
  });
  
  storeRightPane.close();
  storeTopWin.close();
  storeHeaderWin.close();
}