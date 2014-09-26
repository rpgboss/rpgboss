function testStatusMenu() {
  var i = -1;
  loopPartyStatusChoice(function(choiceIdx) {
    ++i;
    if (i == 0) {
      assert(choiceIdx == 0);
      return true;
    } else {
      assert(choiceIdx == 1);
      return false;
    }
  });
  
  waiter.dismiss();
}