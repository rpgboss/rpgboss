function testStatusMenu() {
  var i = -1;
  var statusMenu = new StatusMenu();
  statusMenu.loopChoice(function(choiceIdx) {
    ++i;
    if (i == 0) {
      assert(choiceIdx == 0);
      return true;
    } else {
      assert(choiceIdx == 3);
      return false;
    }
  });
  
  waiter.dismiss();
}