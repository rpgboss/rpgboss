package rpgboss.model.resource

import rpgboss.model._

object ResourceConstants {
  def defaultRcDir = "defaultrc"
  def testRcDir = "testrc"

  def getRcList(dirname: String) = {
    val rcStream = getClass.getClassLoader.getResourceAsStream(
        "%s/enumerated.txt".format(dirname))
    io.Source.fromInputStream(rcStream).getLines().toList
  }

  lazy val defaultRcList = getRcList("defaultrc")
  lazy val testRcList = getRcList("testrc")

  def systemStartScript = "sys/start.js"
  def systemStartCall = "start()"

  def defaultBattleback = "sys/Etolier/01sougen.jpg"
  def battlerTarget = "sys/Redshrike/goblinrider.png"

  def defaultBattleMusic =
    Some(SoundSpec("sys/sean_m_stephens/Battle1.mid"))

  def defaultTilesets = Array(
    "sys/Refmap-TileA5.png",
    "sys/Refmap-TileB.png",
    "sys/Refmap-TileC.png",
    "sys/Refmap-TileD.png",
    "sys/Refmap-TileE.png")

  def defaultAutotiles = Array(
    "sys/Refmap-A1-0-0-A.png",
    "sys/Refmap-A1-0-0-B.png",
    "sys/Refmap-A1-0-1-A.png",
    "sys/Refmap-A1-0-1-B.png",
    "sys/Refmap-A1-1-0-A.png",
    "sys/Refmap-A1-1-0-B.png",
    "sys/Refmap-A1-1-1-A.png",
    "sys/Refmap-A1-1-1-B.png",
    "sys/Refmap-A1-2-0-A.png",
    "sys/Refmap-A1-2-0-B.png",
    "sys/Refmap-A1-2-1-A.png",
    "sys/Refmap-A1-2-1-B.png",
    "sys/Refmap-A1-3-0-A.png",
    "sys/Refmap-A1-3-0-B.png",
    "sys/Refmap-A1-3-1-A.png",
    "sys/Refmap-A1-3-1-B.png",
    "sys/Refmap-A2-0-0.png",
    "sys/Refmap-A2-0-1.png",
    "sys/Refmap-A2-0-2.png",
    "sys/Refmap-A2-0-3.png",
    "sys/Refmap-A2-0-4.png",
    "sys/Refmap-A2-0-5.png",
    "sys/Refmap-A2-0-6.png",
    "sys/Refmap-A2-0-7.png",
    "sys/Refmap-A2-1-0.png",
    "sys/Refmap-A2-1-1.png",
    "sys/Refmap-A2-1-2.png",
    "sys/Refmap-A2-1-3.png",
    "sys/Refmap-A2-1-4.png",
    "sys/Refmap-A2-1-5.png",
    "sys/Refmap-A2-1-6.png",
    "sys/Refmap-A2-1-7.png",
    "sys/Refmap-A2-2-0.png",
    "sys/Refmap-A2-2-1.png",
    "sys/Refmap-A2-2-2.png",
    "sys/Refmap-A2-2-3.png",
    "sys/Refmap-A2-2-4.png",
    "sys/Refmap-A2-2-5.png",
    "sys/Refmap-A2-2-6.png",
    "sys/Refmap-A2-2-7.png",
    "sys/Refmap-A2-3-0.png",
    "sys/Refmap-A2-3-1.png",
    "sys/Refmap-A2-3-2.png",
    "sys/Refmap-A2-3-3.png",
    "sys/Refmap-A2-3-4.png",
    "sys/Refmap-A2-3-5.png",
    "sys/Refmap-A2-3-6.png",
    "sys/Refmap-A2-3-7.png",
    "sys/Refmap-A3-0-0-A.png",
    "sys/Refmap-A3-0-1-A.png",
    "sys/Refmap-A3-0-2-A.png",
    "sys/Refmap-A3-0-3-A.png",
    "sys/Refmap-A3-0-4-A.png",
    "sys/Refmap-A3-0-5-A.png",
    "sys/Refmap-A3-0-6-A.png",
    "sys/Refmap-A3-0-7-A.png",
    "sys/Refmap-A3-1-0-A.png",
    "sys/Refmap-A3-1-1-A.png",
    "sys/Refmap-A3-1-2-A.png",
    "sys/Refmap-A3-1-3-A.png",
    "sys/Refmap-A3-1-4-A.png",
    "sys/Refmap-A3-1-5-A.png",
    "sys/Refmap-A3-1-6-A.png",
    "sys/Refmap-A3-1-7-A.png",
    "sys/Refmap-A3-2-0-A.png",
    "sys/Refmap-A3-2-1-A.png",
    "sys/Refmap-A3-2-2-A.png",
    "sys/Refmap-A3-2-3-A.png",
    "sys/Refmap-A3-2-4-A.png",
    "sys/Refmap-A3-2-5-A.png",
    "sys/Refmap-A3-2-6-A.png",
    "sys/Refmap-A3-2-7-A.png",
    "sys/Refmap-A3-3-0-A.png",
    "sys/Refmap-A3-3-1-A.png",
    "sys/Refmap-A3-3-2-A.png",
    "sys/Refmap-A3-3-3-A.png",
    "sys/Refmap-A3-3-4-A.png",
    "sys/Refmap-A3-3-5-A.png",
    "sys/Refmap-A3-3-6-A.png",
    "sys/Refmap-A3-3-7-A.png",
    "sys/Refmap-A4-0-0.png",
    "sys/Refmap-A4-0-1.png",
    "sys/Refmap-A4-0-2.png",
    "sys/Refmap-A4-0-3.png",
    "sys/Refmap-A4-0-4.png",
    "sys/Refmap-A4-0-5.png",
    "sys/Refmap-A4-0-6.png",
    "sys/Refmap-A4-0-7.png",
    "sys/Refmap-A4-1-0.png",
    "sys/Refmap-A4-1-1.png",
    "sys/Refmap-A4-1-2.png",
    "sys/Refmap-A4-1-3.png",
    "sys/Refmap-A4-1-4.png",
    "sys/Refmap-A4-1-5.png",
    "sys/Refmap-A4-1-6.png",
    "sys/Refmap-A4-1-7.png",
    "sys/Refmap-A4-2-0.png",
    "sys/Refmap-A4-2-1.png",
    "sys/Refmap-A4-2-2.png",
    "sys/Refmap-A4-2-3.png",
    "sys/Refmap-A4-2-4.png",
    "sys/Refmap-A4-2-5.png",
    "sys/Refmap-A4-2-6.png",
    "sys/Refmap-A4-2-7.png",
    "sys/Refmap-A4-3-0.png",
    "sys/Refmap-A4-3-1.png",
    "sys/Refmap-A4-3-2.png",
    "sys/Refmap-A4-3-3.png",
    "sys/Refmap-A4-3-4.png",
    "sys/Refmap-A4-3-5.png",
    "sys/Refmap-A4-3-6.png",
    "sys/Refmap-A4-3-7.png",
    "sys/Refmap-A4-4-0.png",
    "sys/Refmap-A4-4-1.png",
    "sys/Refmap-A4-4-2.png",
    "sys/Refmap-A4-4-3.png",
    "sys/Refmap-A4-4-4.png",
    "sys/Refmap-A4-4-5.png",
    "sys/Refmap-A4-4-6.png",
    "sys/Refmap-A4-4-7.png",
    "sys/Refmap-A4-5-0.png",
    "sys/Refmap-A4-5-1.png",
    "sys/Refmap-A4-5-2.png",
    "sys/Refmap-A4-5-3.png",
    "sys/Refmap-A4-5-4.png",
    "sys/Refmap-A4-5-5.png",
    "sys/Refmap-A4-5-6.png",
    "sys/Refmap-A4-5-7.png")

  def getProjectDataStartup = ProjectDataStartup(
    startingLoc = MapLoc(RpgMap.generateName(1), 5.5f, 5.5f),
    startingParty = Array(0),

    titlePic = "sys/LordSpirit.jpg",
    titleMusic =
      Some(SoundSpec("sys/sean_m_stephens/TitleMoon.mid")),

    gameOverPic = "sys/GameOver.png",
    gameOverMusic =
      Some(SoundSpec("sys/aaron_mcdonald/Macbeth - Cue 2.mid")),

    screenW = 640,
    screenH = 480,
    fullscreen = false,
    windowIcon = "sys/icon.png",

    windowskin = "sys/LastPhantasmScanlines.png",
    msgfont = "sys/VeraMono.ttf",
    fontsize = 24,

    stringInputCharacters =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 -._",

    soundCursor =
      Some(SoundSpec("sys/rpgboss-menu/MenuCursor.ogg")),
    soundSelect =
      Some(SoundSpec("sys/rpgboss-menu/MenuSelect.ogg")),
    soundCancel =
      Some(SoundSpec("sys/rpgboss-menu/MenuCancel.ogg")),
    soundCannot =
      Some(SoundSpec("sys/rpgboss-menu/MenuCannot.ogg")))

  def defaultCharacters = Array(
    Character("Char0", sprite = Some(SpriteSpec("sys/vx_chara01_a.png", 0))),
    Character("Char1", sprite = Some(SpriteSpec("sys/vx_chara01_a.png", 1))),
    Character("Char2", sprite = Some(SpriteSpec("sys/vx_chara01_a.png", 2))),
    Character("Char3", sprite = Some(SpriteSpec("sys/vx_chara01_a.png", 3))),
    Character("Char4", sprite = Some(SpriteSpec("sys/vx_chara01_a.png", 4))))

  def defaultSpriteSpec = SpriteSpec("sys/vx_chara01_a.png", 0)

  def globalsScript = "sys/globals.js"

  def mainScript = "sys/main.js"

  def menuScript = "sys/menu.js"

  def timerScript = "sys/timer.js"

  def transitionsScript = "sys/transitions.js"
}