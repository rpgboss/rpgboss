rpgboss-editor
==============

RPG game editor. Runs on Windows, Linux, and Mac. Authored games eventually will run on mobile platforms also. Based on libgdx.

Licensed under AGPL3.

Building
--------

Below directions assume a POSIX environment. It should still work on Windows (I've done it), but you may need to modify the directions slightly.

Pre-requisites:

+  git
+  sbt - http://www.scala-sbt.org/
+  Java 6, 7, or 8.

Build instructions:

1. Check that your Java install is working. It can be Java 6, 7, or 8. If this does not work, make sure Java is installed an in your PATH.
    ```
    $ java -version                                                                                                                                              
    java version "1.7.0_55"
    OpenJDK Runtime Environment (IcedTea 2.4.7) (ArchLinux build 7.u55_2.4.7-1-x86_64)
    OpenJDK 64-Bit Server VM (build 24.51-b03, mixed mode)
    ```

2. Clone this repo and enter it.
    ```
    $ git clone https://github.com/tommycli/rpgboss.git
    $ cd rpgboss
    ```

3. Run sbt. You will be put in the editor subproject by default. Compile and package it.
    ```
    $ sbt
    > compile
    > package
    ```

4. You can run the editor from the sbt prompt also. Note: This mode of running currently does not support test playing games with the "Test" button. Use Eclipse or run a packaged binary instead.
    ```
    > run
    ```

5. You can generate Eclipse IDE project files. It's what I use, and supports test playing. Your eclipse project files will probably get out of date when dependencies change. You will then have to rerun this command and refresh your project in Eclipse.
    ```
    > eclipse
    ```

Commiting and Automated tests
-----------------------------

Automated tests are how rpgboss verifies that changes don't break existing functionality. Run automated tests before committing! 

1. Run automated tests with:
    ```
    $ sbt "project common" test
    ```

Packaging into binaries
-----------------------

Prerequisites:

+ launch4j

Instructions:

1. Enter the repository and run the package shell script.
    ```
    $ cd rpgboss
    $ ./package/package.sh
    ```

2. Find your binaries in:
    ```
    $ ls package/target/
    rpgboss-0.1-SNAPSHOT.exe  rpgboss-0.1-SNAPSHOT.tar.gz
    ```

Misc
----

Please use LF line endings. This will allow for CRLF line endings under Windows, but auto-convert to LF on checkin.
    ```
    $ git config --global core.autocrlf input
    ```

Project structure
-----------------

+ **Bottom line up front** - Starting *rpgboss.editor.RpgDesktop* as the 'main' class will launch the editor, which will allow you to access the test player.

+ **common** - Contains the models, the game player, and automated tests. 
  
  The models are defined in rpgboss.model and its subpackages.

  The player is defined in rpgboss.player and its subpackages. The player is based on libgdx. Please look at the [libgdx wiki tutorial](https://github.com/libgdx/libgdx/wiki/A-simple-game) to understand the implementation structure.
  
  rpgboss.player.MyGame contains the ApplicationListener, which contains all the player logic. It doesn't have an explicit main-game-loop inside, but is called by libgdx's loop.
  
  The desktop player has a 'main' class is at rpgboss.player.LwjglPlayer. It requires the path to the game project directory as its first argument to run the game.

+ **editor** - Contains the Swing based editor. Contains no game logic, just UI to edit the model defined in the *common* package. The 'main' class is rpgboss.editor.RpgDesktop.

Development Notes
-----------------

+ If you add a resource type, make sure you add it to the list at Resource.resourceTypes.

+ If you add an rpgboss.model.event.EventCmd, be sure to add it to EventCmd.types.

+ Use Array as the collection type. It works in tests now due to a custom DeepEqualMatcher, is performant, and well supported in serialization.

Material
--------

### Animations

Animations are in the same format as RPG Maker VX and XP. They should be comprised of square tiles 192px wide and tall. Each row may have up to 5 tiles. There may be up to 16 rows.

http://spieralwind.tuzikaze.com/main/index.html

fire(7,8,9,10) ice(1,2,3,4,7) water(4,5,6,8,11) wind(1,2,3)

Splash animation by Julien Jorge http://opengameart.org/content/water-splash (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

water(2)

Julian Xin raveolutionx@gmail.com (CC-BY-SA 4.0)  http://creativecommons.org/licenses/by-sa/4.0/

fire(1,2,3,4,5.6) ice(5,6,8) water(1,3,7,9,10) wind(4,5,6,7)

### Autotiles, Spritesets, Tilesets

http://www.tekepon.net/fsm/
http://www.tekepon.net/fsm/modules/refmap/index.php?mode=rules

USE Internet Archieve for Dead website links!

### Battlers

http://opengameart.org/content/sideview-pixel-art-rpg-enemy-sprites under creative commons.

### Iconsets

420__Pixel_Art__Icons_for_RPG_by_Ails.png:
http://ails.deviantart.com/art/420-Pixel-Art-Icons-for-RPG-129892453

### Faces

### Window Skin

http://rpgmakertimes.agilityhoster.com/2011/02/final-fantasy-i-xpvx-windowskin/

### Picture

LordSpirit.jpg:
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3701

defaultrc_battleback/crownlesswish_rrr.jpg
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3769
http://crownlesswish.deviantart.com/

### Sounds

Generated using bfxr at:
http://www.bfxr.net/

http://opengameart.org/content/rpg-sound-pack

http://opengameart.org/content/37-hitspunches

### Music

Default music by Aaron McDonald. Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0). http://creativecommons.org/licenses/by-sa/4.0/

### Material Rules

Autotiles are individual files in Rpg Maker VX format.
Spritesets are in Rpg Maker VX format.
Tilesets only constrained to be comprised of 32x32 square tiles.
Iconsets can be any size, but you will need to specify the tile-size when you import it.

Windowskins are in the Rpg Maker XP format.

