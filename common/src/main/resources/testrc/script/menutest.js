function testStatusMenu() {
  loopPartyStatusChoice(function(choiceIdx) {
    assert(choiceIdx == 0);
    waiter.dismiss();
    return false;
  });
}