// here you can add your own scripts

/*
// HP Bar example, uncomment to use
var stats = game.getBattleStats(0, -1, -1);
var characterHps = game.getIntArray(game.CHARACTER_HPS());

while(true) {

	if(game.getInt("hpVisible")==1) {

		var hp = characterHps[0].toString(),
				maxhp = stats.current().mhp();

		if(hp<0) hp = 0;
		var calcHP = Math.ceil( (hp/maxhp)*10 )*10;
		game.log(calcHP);
		game.showPicture(20, "hp-left-"+calcHP+".png", game.layoutWithOffset(0,1,1.0,1.0,0,0));
		
		game.drawText(1,"HP: "+hp + " / " + maxhp,45,35,game.color(255,255,255,1),0.6);

	}
	
	game.sleep(1);
}
*/