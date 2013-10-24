rpgboss-editor
==============

RPG game editor. Runs on Windows, Linux, and Mac. Authored games eventually will run on mobile platforms also. Based on libgdx.

Licensed under AGPL3.

Building
--------

Below directions assume a POSIX environment.

Pre-requisites:

+  git
+  sbt - http://www.scala-sbt.org/
+  Java

Build instructions:

1. Create a directory to hold rpgboss and its dependencies.

```
mkdir rpgboss
cd rpgboss
```

2. The build depends on https://github.com/ritschwumm/xsbt-webstart. Satisfy this by:

```
git clone https://github.com/ritschwumm/xsbt-classpath.git
cd xsbt-classpath
sbt publish-local
cd ..
git clone https://github.com/ritschwumm/xsbt-webstart.git
cd xsbt-webstart
sbt publish-local
cd ..
```

3. Clone this repo and enter it.

```
git clone https://github.com/tommycli/rpgboss.git
cd rpgboss
```

4. Run sbt. Once you are in the sbt prompt, run these commands to pull in extra dependencies:

```
sbt
> update-gdx
> update-libs
```

5. You should still be in the sbt prompt. Switch to the 'editor' subproject, compile, and package it.

```
> project editor
> compile
> package
```

6. You can run the editor from the sbt prompt also.

```
> run
```

Packaging into a cross-platform binary
--------------------------------------

To package into a cross-platform binary, we use launch4j.

1. Install launch4j.

Material
--------

### Autotiles, Spritesets, Tilesets

http://www.tekepon.net/fsm/
http://www.tekepon.net/fsm/modules/refmap/index.php?mode=rules

### Iconsets

420__Pixel_Art__Icons_for_RPG_by_Ails.png:
http://ails.deviantart.com/art/420-Pixel-Art-Icons-for-RPG-129892453

### Faces

### Window Skin

http://rpgmakertimes.agilityhoster.com/2011/02/final-fantasy-i-xpvx-windowskin/

### Picture

LordSpirit.jpg:
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3701

### Sounds

Generated using bfxr at:
http://www.bfxr.net/

### Material Rules

Autotiles are individual files in Rpg Maker VX format.
Spritesets are in Rpg Maker VX format.
Tilesets only constrained to be comprised of 32x32 square tiles.
Iconsets can be any size, but you will need to specify the tile-size when you import it.

Windowskins are in the Rpg Maker XP format.

