function testStatusMenu() {
  var i = -1;
  var statusMenu = new StatusMenu();
  statusMenu.loopCharacterChoice(function(characterId) {
    ++i;
    if (i == 0) {
      assert(characterId == 0);
      return true;
    } else {
      assert(characterId == 3);
      return false;
    }
  });
  
  waiter.dismiss();
}