rpgboss-editor
==============

[![Build Status](https://travis-ci.org/rpgboss/rpgboss.svg?branch=master)](https://travis-ci.org/rpgboss/rpgboss)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/rpgboss/rpgboss.svg)](http://isitmaintained.com/project/rpgboss/rpgboss "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/rpgboss/rpgboss.svg)](http://isitmaintained.com/project/rpgboss/rpgboss "Percentage of issues still open")

RPG game editor. Runs on Windows, Linux, and Mac. Authored games eventually will run on mobile platforms also. Based on libgdx.

Licensed under AGPL3.

Building
--------

Below directions assume a POSIX environment. It should still work on Windows (I've done it), but you may need to modify the directions slightly.

Pre-requisites:

+  git
+  Java 6, 7, or 8. OR Openjdk-7, Openjdk-8 (8u40-b27-1 or more)

Build instructions:

1. Check that your Java install is working. It can be Java 6, 7, or 8. If this does not work, make sure Java is installed an in your PATH.
    ```
    $ java -version                                                                                                                                              
    java version "1.7.0_55"
    OpenJDK Runtime Environment (IcedTea 2.4.7) (ArchLinux build 7.u55_2.4.7-1-x86_64)
    OpenJDK 64-Bit Server VM (build 24.51-b03, mixed mode)
    ```

2. Fork this repository into your own GitHub account. Clone your fork and enter it.
    ```
    $ git clone https://github.com/yourusername/rpgboss.git
    $ cd rpgboss
    ```

3. Run the included gradlew script to build and run the program:
    ```
    Linux / Mac:
    $ ./gradlew run

    Windows:
    gradlew.bat run
    ```

4. You should be able to import the Gradle project into Eclipse or another IDE.
   See: https://github.com/libgdx/libgdx/wiki/Gradle-and-Eclipse

Commiting and Automated tests
-----------------------------

Automated tests are how rpgboss verifies that changes don't break existing functionality. Run automated tests before committing!

1. Run automated tests with:
    ```
    $ ./gradlew test
    ```

Packaging into binaries
-----------------------

Prerequisites:

+ launch4j
  + launch4j may require 32-bit libraries installed. On Ubuntu 14.04:
    ```
    sudo apt-get install lib32z1 lib32ncurses5 lib32bz2-1.0 lib32stdc++6
    ```

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

fire(7,8,9,10) ice(1,2,3,4,7,9) water(4,5,6,8,11) wind(1,2,3) earth(1,2) electric(1,2,5) life(1,2,3,4,5) weapon(1,2,3,5,6,7,8,9,10,11)

Julien Jorge http://opengameart.org/content/water-splash (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

water(2)

Julian Xin raveolutionx@gmail.com (CC-BY 4.0)  http://creativecommons.org/licenses/by/4.0/

fire(1,2,3,4,5.6) ice(5,6,8) water(1,3,7,9,10) wind(4,5,6,7) earth(3,4,5,6) electric(4,6,7) weapon(4)

Martin Jelinek (jelinek.cz@gmail.com) | www.nyrthos.com

electric(3)

### Autotiles, Spritesets, Tilesets

http://www.tekepon.net/fsm/
http://www.tekepon.net/fsm/modules/refmap/index.php?mode=rules

Vehicles:
Derived from: Art by DualR. Commissioned by OpenGameArt.org (http://opengameart.org)

USE Internet Archieve for Dead website links!

### Battlers

http://media.ryzom.com/ (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(Ryzom)

http://darts.kirara.st/m/ (CC-BY-ND) http://creativecommons.org/licenses/by-nd/4.0/

(Kirara)

http://www.junkie-chain.jp/kiyaku.html

(Junkie)

Justin Nichol http://opengameart.org/users/justin-nichol (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(JustinNichol)

P0ss http://opengameart.org/content/reaper-of-the-post-urban-jungle-concept-art (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(P0ss)

Killyoverdrive http://opengameart.org/users/killyoverdrive (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(Killyoverdrive)

richtaur http://opengameart.org/content/t-rex-02 (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Richtaur)

Benalene http://opengameart.org/content/fire-and-ice-elementals (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Benalene)

godlesshenk http://godlesshenk.deviantart.com/gallery/ (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Godlesshenk)

Buch http://opengameart.org/content/turtle-like-beast-concept (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Buch)

ramtam http://opengameart.org/content/creature-sprites (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(Ramtam)

Isaac Bird http://nobody00000000.deviantart.com/gallery/ (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(IsaacBird)

Ancient Beast  http://www.ancientbeast.com/ (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/

(AncientBeast)

Katarzyna Zalecka http://www.ancientbeast.com/ (CC-BY-SA 3.0) http://creativecommons.org/licenses/by-sa/3.0/
http://kasia88.deviantart.com; Gabriel Verdon (Magmaspawn) http://www.gabrielverdon.com

(Katarzyna)

Stephen "Redshrike" Challener http://opengameart.org/users/redshrike (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Redshrike)

Blarumyrran http://opengameart.org/content/rpg-enemies-11-dragons (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Blarumyrran)

Sharm http://opengameart.org/content/rpg-enemies-11-dragons (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Sharm)

Zabin http://opengameart.org/content/rpg-enemies-11-dragons (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Zabin)

Surt http://opengameart.org/users/surt (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(Surt)

MrBeast http://opengameart.org/users/MrBeast (CC-BY 3.0) http://creativecommons.org/licenses/by/3.0/

(MrBeast)

Minus Create http://silsec.sakura.ne.jp/WolfRPGEditor/BBS/BBS_patio.cgi?mode=view&no=94

(MinusCreate)

Normal Army http://silsec.sakura.ne.jp/WolfRPGEditor/BBS/BBS_patio.cgi?mode=view&no=94

(NormalArmy)

C+ http://silsec.sakura.ne.jp/WolfRPGEditor/BBS/BBS_patio.cgi?mode=view&no=94

(C+)

### Iconsets

420__Pixel_Art__Icons_for_RPG_by_Ails.png:
http://ails.deviantart.com/art/420-Pixel-Art-Icons-for-RPG-129892453

### Faces

Faces by Mackie also of FSM.

### Window Skin

http://rpgmakertimes.agilityhoster.com/2011/02/final-fantasy-i-xpvx-windowskin/

### Picture

LordSpirit.jpg:
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3701

### Battle Background

##### Side View Battle Background

Julian Xin raveolutionx@gmail.com (CC-BY 4.0)  http://creativecommons.org/licenses/by/4.0/

(Xin)

http://etolier.webcrow.jp/sozai.html

(Etolier)

defaultrc_battleback/crownlesswish_rrr.jpg
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3769
http://crownlesswish.deviantart.com/

(Crownlesswish)

### Sounds

Generated using bfxr at:
http://www.bfxr.net/

http://opengameart.org/content/rpg-sound-pack

http://opengameart.org/content/37-hitspunches

http://opengameart.org/content/spell-sounds-starter-pack

### Music

Default music by Aaron McDonald and Sean M. Stephens.

Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0). http://creativecommons.org/licenses/by-sa/4.0/

Other Credits
-------------

Some program icons from:
Hendrik Weiler: https://github.com/hendrik-weiler
https://www.iconfinder.com/icons/284087/edit_editor_pen_pencil_write_icon#size=128
https://www.iconfinder.com/icons/174644/bucket_paint_icon#size=128

Material Rules
--------------

Autotiles are individual files in Rpg Maker VX format.
Spritesets are in Rpg Maker VX format.
Tilesets only constrained to be comprised of 32x32 square tiles.
Iconsets can be any size, but you will need to specify the tile-size when you import it.

Windowskins are in the Rpg Maker XP format.
