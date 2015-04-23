package rpgboss.model.event

import rpgboss.model._
import rpgboss.player._
import org.json4s.TypeHints
import org.json4s.ShortTypeHints
import EventCmd._
import EventJavascript._
import rpgboss.player.entity.PrintingTextWindowOptions
import rpgboss.player.entity.WindowText
import scala.collection.mutable.ArrayBuffer
import rpgboss.lib.Utils
import rpgboss.lib.ArrayUtils
import rpgboss.lib.Layout
import rpgboss.player.entity.Entity

trait EventCmd extends HasScriptConstants {
  def sections: Array[CodeSection]

  /**
   *  Returns a copy of this EventCmd with a new set of inner commands placed
   *  in section |commandListI|.
   */
  def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    throw new NotImplementedError
  }

  val toJs: Array[String] =
    sections.flatMap(_.toJs)

  def getParameters(): List[EventParameter[_]] = Nil
}

object EventCmd {
  case class CommandList(cmds: Array[EventCmd],
    indent: Int) extends CodeSection {
    def toJs = cmds
      .map(_.toJs) // Convert to JS
      .flatten
      .map(("  " * indent) + _) // Indent by 2 spaces
  }

  // Used to deserialize legacy names.
  case object EventRenameHints extends TypeHints {
    val hints: List[Class[_]] = Nil
    def hintFor(clazz: Class[_]) = sys.error("No hints provided")
    def classFor(hint: String) = hint match {
      case "SetEvtState" => Some(classOf[SetEventState])
      case _ => None
    }
  }

  val hints = ShortTypeHints(List(
    classOf[AddRemoveItem],
    classOf[AddRemoveGold],
    classOf[BreakLoop],
    classOf[CallMenu],
    classOf[CallSaveMenu],
    classOf[Comment],
    classOf[ClearTimer],
    classOf[EquipItem],
    classOf[ExitGame],
    classOf[FadeIn],
    classOf[FadeOut],
    classOf[GameOver],
    classOf[GetChoice],
    classOf[GetEntityInfo],
    classOf[GetKeyInput],
    classOf[GetNumberInput],
    classOf[GetStringInput],
    classOf[GiveExperience],
    classOf[HealOrDamage],
    classOf[HidePicture],
    classOf[IfCondition],
    classOf[IncrementEventState],
    classOf[LockPlayerMovement],
    classOf[SetCharacterLevel],
    classOf[Sleep],
    classOf[ModifyParty],
    classOf[MoveCamera],
    classOf[MoveEvent],
    classOf[OpenStore],
    classOf[PlayAnimation],
    classOf[PlayMusic],
    classOf[PlaySound],
    classOf[Return],
    classOf[RunJs],
    classOf[SetCameraFollow],
    classOf[SetCharacterName],
    classOf[SetEventsEnabled],
    classOf[SetEventSpeed],
    classOf[SetEventState],
    classOf[SetGlobalInt],
    classOf[SetTransition],
    classOf[SetTimer],
    classOf[SetLocalInt],
    classOf[SetMenuEnabled],
    classOf[SetWindowskin],
    classOf[StopSound],
    classOf[ShowText],
    classOf[ShowPicture],
    classOf[StartBattle],
    classOf[StopMusic],
    classOf[Teleport],
    classOf[TintScreen],
    classOf[WeatherEffects],
    classOf[WhileLoop])) + EventRenameHints
}

case class AddRemoveItem(
  var add: Boolean = true,
  itemId: IntParameter = IntParameter(),
  quantity: IntParameter = IntParameter(1))
  extends EventCmd {
  def qtyDelta =
    RawJs(if (add) quantity.rawJs.exp else "%s * -1".format(quantity.rawJs.exp))
  def sections = singleCall("game.addRemoveItem", itemId, qtyDelta)

  override def getParameters() = List(itemId, quantity)
}

case class AddRemoveGold(
  var add: Boolean = true,
  quantity: IntParameter = IntParameter())
  extends EventCmd {
  def qtyDelta =
    RawJs(if (add) quantity.rawJs.exp else "%s * -1".format(quantity.rawJs.exp))
  def sections = singleCall("game.addRemoveGold", qtyDelta)

  override def getParameters() = List(quantity)
}

case class BreakLoop() extends EventCmd {
  def sections = Array(PlainLines(Array("break;")))
}

case class Comment(var commentString: String = "") extends EventCmd {
  def sections = Array(PlainLines(commentString.split("\n").map("// " + _)))
}

case class EquipItem(
  characterId: IntParameter = IntParameter(),
  slotId: IntParameter = IntParameter(),
  itemId: IntParameter = IntParameter(),
  var equip: Boolean = true) extends EventCmd {
  def sections =
    if (equip)
      singleCall("game.equipItem", characterId, slotId, itemId)
    else
      singleCall("game.equipItem", characterId, slotId, -1)
}

case class ExitGame() extends EventCmd {
  def sections =
    Array(PlainLines(Array(jsStatement("game.gameOver"), "return;")))
}

/**
 * @param   innerCmds   Has one more element than choices to account for the
 *                      default case.
 */
case class GetChoice(
  var question: Array[String] = Array(),
  var choices: Array[String] = Array("Yes", "No"),
  var allowCancel: Boolean = false,
  var innerCmds: Array[Array[EventCmd]] = Array(Array(), Array(), Array()),
  var customFace: Option[FaceSpec] = None,
  var useCharacterFace: Boolean = false,
  var characterId: Int = 0) extends EventCmd {
  def sections = {
    val buf = new ArrayBuffer[CodeSection]()

    def caseSections(caseLabel: String, code: Array[EventCmd]) = Array(
      PlainLines(Array("  %s:".format(caseLabel))),
      CommandList(code, 2),
      PlainLines(Array("    break;")))

    val questionOptions = if (customFace.isDefined || useCharacterFace) {
      PrintingTextWindowOptions(
          useCustomFace = customFace.isDefined,
          faceset = customFace.map(_.faceset).getOrElse(""),
          faceX = customFace.map(_.faceX).getOrElse(0),
          faceY = customFace.map(_.faceY).getOrElse(0),
          useCharacterFace = useCharacterFace,
          characterId = characterId)
    }

    buf += PlainLines(Array(
      "switch (game.getChoice(%s, %s, %s, %s)) {".format(
        EventJavascript.toJs(question),
        EventJavascript.toJs(choices),
        EventJavascript.toJs(allowCancel),
        EventJavascript.toJs(questionOptions))))

    for (i <- 0 until choices.size) {
      caseSections("case %d".format(i), innerCmds(i)).foreach(buf += _)
    }

    caseSections("default", innerCmds.last).foreach(buf += _)

    buf += PlainLines(Array("}"))

    buf.toArray
  }

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    val newArray = ArrayUtils.normalizedAry(
      innerCmds, choices.size + 1, choices.size + 1, () => Array[EventCmd]())
    newArray.update(commandListI, newInnerCmds)
    copy(innerCmds = newArray)
  }
}

case class GetKeyInput(
  var storeInVariable: String = "globalVariableName",
  var capturedKeys: Array[Int] = Array(MyKeys.OK, MyKeys.Cancel))
extends EventCmd {
  def sections = singleCall("game.setInt", storeInVariable,
      RawJs(jsCall("game.getKeyInput", capturedKeys).exp))
}

case class GetNumberInput(
  var message: String = "Enter number:",
  var storeInVariable: String = "globalVariableName",
  digits: IntParameter = IntParameter(5),
  initial: IntParameter = IntParameter(0))
extends EventCmd {
  def sections = singleCall("game.setInt", storeInVariable,
      RawJs(jsCall("game.getNumberInput", message, digits, initial).exp))
}

case class GetStringInput(
  var message: String = "Enter string:",
  var storeInVariable: String = "globalVariableName",
  maxLength: IntParameter = IntParameter(5),
  initial: StringParameter = StringParameter("")) extends EventCmd {
  def sections = singleCall("game.setString", storeInVariable,
      RawJs(jsCall("game.getStringInput", message, maxLength, initial).exp))
}

case class GiveExperience(
  var wholeParty: Boolean = true,
  characterId: IntParameter = IntParameter(),
  experience: IntParameter = IntParameter(1000),
  var showNotifications: Boolean = true) extends EventCmd {
  def sections = {
    if (wholeParty)
      singleCall("game.givePartyExperience", experience, showNotifications)
    else
      singleCall("game.giveCharacterExperience", characterId, experience,
          showNotifications)
  }
}

case class HealOrDamage(
  var heal: Boolean = true,
  var wholeParty: Boolean = true,
  var characterId: Int = 0,
  var hpPercentage: Float = 1.0f,
  var mpPercentage: Float = 1.0f,
  var removeStatusEffects: Boolean = true) extends EventCmd {
  def sections = {
    if (heal) {
      if (wholeParty) {
        singleCall("game.healParty", hpPercentage, mpPercentage,
          removeStatusEffects)
      } else {
        singleCall("game.healCharacter", characterId, hpPercentage,
          mpPercentage, removeStatusEffects)
      }
    } else {
      if (wholeParty) {
        singleCall("game.damageParty", hpPercentage, mpPercentage)
      } else {
        singleCall("game.damageCharacter", characterId, hpPercentage,
          mpPercentage)
      }
    }
  }
}

case class WeatherEffects(var weatherTypeId: Int = WeatherTypes.default.id)
  extends EventCmd {
  def sections = singleCall("game.setWeather", weatherTypeId)
}

case class IfCondition(
  var conditions: Array[Condition] = Array(),
  var elseBranch: Boolean = false,
  var trueCmds: Array[EventCmd] = Array(),
  var elseCmds: Array[EventCmd] = Array())
  extends EventCmd {
  def sections = {
    val buf = new ArrayBuffer[CodeSection]()

    buf += PlainLines(Array("if (%s) {".format(
      Condition.allConditionsExp(conditions).exp)))
    buf += CommandList(trueCmds, 1)

    if (elseBranch) {
      buf += PlainLines(Array("} else {"))
      buf += CommandList(elseCmds, 1)
    }

    buf += PlainLines(Array("}"))

    buf.toArray
  }

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    assert(commandListI == 0 || commandListI == 1)
    if (commandListI == 0) {
      copy(trueCmds = newInnerCmds)
    } else {
      copy(elseCmds = newInnerCmds)
    }
  }
}

case class HidePicture(
  slot: IntParameter = IntParameter(PictureSlots.ABOVE_MAP)) extends EventCmd {
  def sections = singleCall("game.hidePicture", slot)
  override def getParameters() = List(slot)
}

case class IncrementEventState() extends EventCmd {
  def sections = singleCall("game.incrementEventState", RawJs("event.id()"))
}

case class LockPlayerMovement(body: Array[EventCmd]) extends EventCmd {
  // TODO: Clean this up. Probably need a better JS DSL.
  def sections = Array(
    PlainLines(Array(
      jsStatement("game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " + 1")),
      RawJs("try {").exp)),
    CommandList(body, 1),
    PlainLines(Array(
      RawJs("} finally {").exp,
      jsStatement("  game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " - 1")),
      RawJs("}").exp)))

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    assert(commandListI == 0)
    copy(body = newInnerCmds)
  }
}

case class ModifyParty(var add: Boolean = true, var characterId: Int = 0)
  extends EventCmd {
  def sections = singleCall("game.modifyParty", add, characterId)
}

case class GetEntityInfo(
  var entitySpec: EntitySpec = EntitySpec(),
  var globalVariableName: String = "",
  var kind: Int = 0) extends EventCmd {

  def executeCommand(kind: Int, playerOrNot: Boolean, eventId: Int): Array[CodeSection] = {

    var expr: RawJs = null

    if (playerOrNot) {

      if (kind == 0) {
        expr = RawJs(jsCall("game.getPlayerInfo").exp + ".x")
      }
      if (kind == 1) {
        expr = RawJs(jsCall("game.getPlayerInfo").exp + ".y")
      }
      if (kind == 2) {
        expr = RawJs(jsCall("game.getPlayerInfo").exp + ".dir")
      }
      if (kind == 3) {
        expr = RawJs(jsCall("game.getPlayerInfo").exp + ".screenX")
      }
      if (kind == 4) {
       expr = RawJs(jsCall("game.getPlayerInfo").exp + ".screenY")
      }
      if (kind == 5) {
       expr = RawJs(jsCall("game.getPlayerInfo").exp + ".screenTopLeftX")
      }
      if (kind == 6) {
       expr = RawJs(jsCall("game.getPlayerInfo").exp + ".screenTopLeftY")
      }
      if (kind == 7) {
       expr = RawJs(jsCall("game.getPlayerInfo").exp + ".width")
      }
      if (kind == 8) {
       expr = RawJs(jsCall("game.getPlayerInfo").exp + ".height")
      }

    } else {

      if (kind == 0) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".x")
      }
      if (kind == 1) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".y")
      }
      if (kind == 2) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".dir")
      }
      if (kind == 3) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".screenX")
      }
      if (kind == 4) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".screenY")
      }
      if (kind == 5) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".screenTopLeftX")
      }
      if (kind == 6) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".screenTopLeftY")
      }
      if (kind == 7) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".width")
      }
      if (kind == 8) {
        expr = RawJs(jsCall("game.getEventInfo", eventId).exp + ".height")
      }
    }

    return singleCall("game.setInt", globalVariableName, expr)
  }

  def sections: Array[CodeSection] = {

    entitySpec match {
      case EntitySpec(which, _, _) if which == WhichEntity.PLAYER.id =>
        return executeCommand(kind, true, WhichEntity.PLAYER.id)
      case EntitySpec(which, _, eventIdx) if which == WhichEntity.THIS_EVENT.id =>
        return executeCommand(kind, false, eventIdx)
      case EntitySpec(which, _, eventIdx) if which == WhichEntity.EVENT_ON_MAP.id =>
        return executeCommand(kind, false, eventIdx)
    }
  }

}

case class MoveEvent(
  var entitySpec: EntitySpec = EntitySpec(),
  var dx: Float = 0f,
  var dy: Float = 0f,
  var affixDirection: Boolean = false,
  var async: Boolean = false) extends EventCmd {
  def sections = entitySpec match {
    case EntitySpec(which, _, _) if which == WhichEntity.PLAYER.id =>
      singleCall("game.movePlayer", dx, dy, affixDirection, async)
    case EntitySpec(which, _, _) if which == WhichEntity.THIS_EVENT.id =>
      singleCall(
        "game.moveEvent", RawJs("event.id()"), dx, dy, affixDirection,
        async)
    case EntitySpec(which, _, eventIdx) if which == WhichEntity.EVENT_ON_MAP.id =>
      singleCall(
        "game.moveEvent", entitySpec.eventId, dx, dy, affixDirection, async)
  }
}

case class OpenStore(
  itemIdsSold: IntArrayParameter = IntArrayParameter(),
  buyPriceMultiplier: FloatParameter = FloatParameter(1.0f),
  sellPriceMultiplier: FloatParameter = FloatParameter(0.5f)) extends EventCmd {
  def sections = singleCall("game.openStore", itemIdsSold, buyPriceMultiplier,
    sellPriceMultiplier)

  override def getParameters() =
    List(itemIdsSold, buyPriceMultiplier, sellPriceMultiplier)
}

case class PlayAnimation(
  var animationId: Int = 0,
  var originId: Int = Origins.default.id,
  var entitySpec: EntitySpec = EntitySpec(),
  var xOffset: Int = 0,
  var yOffset: Int = 0,
  var speedScale: Float = 1.0f,
  var sizeScale: Float = 1.0f) extends EventCmd {
  def sections = Origins(originId) match {
    case Origins.SCREEN_TOP_LEFT =>
      singleCall("game.playAnimation", animationId, xOffset, yOffset,
        speedScale, sizeScale)
    case Origins.SCREEN_CENTER =>
      singleCall("game.playAnimation", animationId,
        applyOperator(RawJs("game.getScreenW() / 2"), " + ",
          RawJs(EventJavascript.toJs(xOffset))),
        applyOperator(RawJs("game.getScreenH() / 2"), " + ",
          RawJs(EventJavascript.toJs(yOffset))),
        speedScale, sizeScale)
    case Origins.ON_ENTITY => {
      entitySpec match {
        case EntitySpec(which, _, _) if which == WhichEntity.PLAYER.id =>
          singleCall("game.playAnimationOnPlayer", animationId, speedScale,
            sizeScale)
        case EntitySpec(which, _, _) if which == WhichEntity.THIS_EVENT.id =>
          singleCall(
            "game.playAnimationOnEvent", animationId, RawJs("event.id()"),
            speedScale, sizeScale)
        case EntitySpec(which, _, eventIdx) if which == WhichEntity.EVENT_ON_MAP.id =>
          singleCall(
            "game.playAnimationOnEvent", animationId, entitySpec.eventId,
            speedScale, sizeScale)
      }
    }
  }
}

case class PlayMusic(
  slot: IntParameter = IntParameter(0),
  var spec: SoundSpec = SoundSpec(),
  var loop: Boolean = true,
  var fadeDuration: Float = 0.5f) extends EventCmd {
  def sections =
    singleCall("game.playMusic", slot, spec.sound, spec.volume, loop,
      fadeDuration)
  override def getParameters() = List(slot)
}

case class PlaySound(var spec: SoundSpec = SoundSpec()) extends EventCmd {
  def sections =
    singleCall("game.playSound", spec.sound, spec.volume, spec.pitch)
}

case class Return() extends EventCmd {
  def sections = Array(PlainLines(Array("return;")))
}

case class RunJs(var scriptBody: String = "") extends EventCmd {
  def sections = Array(PlainLines(Array(scriptBody.split("\n"): _*)))
}

case class SetCameraFollow(
  var entitySpec: EntitySpec = EntitySpec(WhichEntity.PLAYER.id))
  extends EventCmd {
  def sections = WhichEntity(entitySpec.whichEntityId) match {
    case WhichEntity.PLAYER =>
      singleCall("game.setCameraFollowPlayer")
    case WhichEntity.EVENT_ON_MAP =>
      singleCall("game.setCameraFollowEvent", entitySpec.eventId)
    case WhichEntity.THIS_EVENT =>
      singleCall("game.setCameraFollowEvent", RawJs("event.id()"))
    case WhichEntity.NONE =>
      singleCall("game.setCameraFollowNone")
  }
}

case class SetCharacterLevel(
  var wholeParty: Boolean = true,
  characterId: IntParameter = IntParameter(),
  level: IntParameter = IntParameter(20)) extends EventCmd {
  def sections = {
    if (wholeParty)
      singleCall("game.setPartyLevel", level)
    else
      singleCall("game.setCharacterLevel", characterId, level)
  }
}

case class SetCharacterName(
  characterId: IntParameter = IntParameter(),
  var getPlayerInput: Boolean = true,
  fixedValue: StringParameter = StringParameter()) extends EventCmd {
  def sections = {
    if (getPlayerInput) {
      singleCall("game.setCharacterName", characterId, RawJs(
          jsCall("game.getCharacterNameFromPlayerInput", characterId).exp))
    } else {
      singleCall("game.setCharacterName", characterId, fixedValue)
    }
  }
}

case class SetEventState(
  var entitySpec: EntitySpec = EntitySpec(),
  var state: Int = 0) extends EventCmd {
  def sections = {
    val (mapName, eventId) = WhichEntity(entitySpec.whichEntityId) match {
      case WhichEntity.THIS_EVENT =>
        (RawJs("event.mapName()"), RawJs("event.id()"))
      case WhichEntity.EVENT_ON_MAP =>
        (RawJs("event.mapName()"), entitySpec.eventId)
      case WhichEntity.EVENT_ON_OTHER_MAP =>
        (entitySpec.mapName, entitySpec.eventId)
    }

    singleCall("game.setEventState", mapName, eventId, state)
  }
}

case class SetEventSpeed(
  var entitySpec: EntitySpec = EntitySpec(),
  var speed: Float = Entity.defaultEntitySpeed) extends EventCmd {
  def sections = entitySpec match {
    case EntitySpec(which, _, _) if which == WhichEntity.PLAYER.id =>
      singleCall("game.setPlayerSpeed", speed)
    case EntitySpec(which, _, _) if which == WhichEntity.THIS_EVENT.id =>
      singleCall(
        "game.setEventSpeed", RawJs("event.id()"), speed)
    case EntitySpec(which, _, eventIdx) if which == WhichEntity.EVENT_ON_MAP.id =>
      singleCall(
        "game.setEventSpeed", entitySpec.eventId, speed)
  }
}

case class SetGlobalInt(
  var key: String = "globalVariableName",
  var operatorId: Int = OperatorType.default.id,
  value1: IntParameter = IntParameter()) extends EventCmd {
  def sections: Array[CodeSection] = {
    val operator = OperatorType(operatorId)
    operator match {
      case OperatorType.Set =>
        singleCall("game.setInt", key, value1)
      case _ =>
        singleCall("game.setInt", key,
          applyOperator(jsCall("game.getInt", key), operator.jsString,
            value1.rawJs))
    }
  }
}

case class SetLocalInt(variableName: String,
  value: EventParameter[_]) extends EventCmd {
  def sections = Array(PlainLines(
    Array("var %s = %s;".format(variableName, value.rawJs.exp))))
}

case class SetWindowskin(var windowskinPath: String = "") extends EventCmd {
  def sections = singleCall("game.setWindowskin", windowskinPath)
}

case class Sleep(var duration: Float = 0) extends EventCmd {
  def sections = singleCall("game.sleep", duration)
}

case class ShowPicture(
  slot: IntParameter = IntParameter(PictureSlots.ABOVE_MAP),
  var picture: String = "",
  layout: Layout = Layout.defaultForPictures,
  var alpha: Float = 1) extends EventCmd {
  def sections =
    singleCall("game.showPicture", slot, picture, layout.toJs(), alpha)
  override def getParameters() = List(slot)
}

case class StartBattle(
  var encounterId: Int = 0,
  var battleMusic: Option[SoundSpec] = None,
  var battleBackground: String = "")
  extends EventCmd {
  def sections = battleMusic.map { music =>
    singleCall("game.startBattle", encounterId, battleBackground,
      music.sound, music.volume)
  }.getOrElse {
    singleCall("game.startBattle", encounterId, battleBackground, "", 1.0f)
  }
}

case class StopMusic(
  slot: IntParameter = IntParameter(0),
  var fadeDuration: Float = 0.5f) extends EventCmd {
  def sections = singleCall("game.stopMusic", slot, fadeDuration)
  override def getParameters() = List(slot)
}

case class Teleport(loc: MapLoc = MapLoc(),
  var transitionId: Int = Transitions.FADE.id) extends EventCmd {
  def sections =
    Array(PlainLines(Array(
        jsStatement("game.teleport", loc.map, loc.x, loc.y, transitionId),
        "return;")))
}

case class SetMenuEnabled(var enabled: Boolean = false) extends EventCmd {
  def sections = {
    singleCall("game.setMenuEnabled", enabled)
  }
}

case class SetEventsEnabled(var enabled: Boolean = false) extends EventCmd {
  def sections = {
    singleCall("game.setEventsEnabled", enabled)
  }
}

case class TintScreen(
  var r: Float = 1.0f,
  var g: Float = 0,
  var b: Float = 0,
  var a: Float = 0.5f,
  var fadeDuration: Float = 1) extends EventCmd {
  def sections =
    singleCall("game.tintScreen", r, g, b, a, fadeDuration)
}

case class WhileLoop(
  var conditions: Array[Condition] = Array(),
  var innerCmds: Array[EventCmd] = Array())
  extends EventCmd {
  def sections = {
    val buf = new ArrayBuffer[CodeSection]()

    buf += PlainLines(Array("while (%s) {".format(
      Condition.allConditionsExp(conditions).exp)))
    buf += CommandList(innerCmds, 1)
    buf += PlainLines(Array("}"))

    buf.toArray
  }

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    assert(commandListI == 0)
    copy(innerCmds = newInnerCmds)
  }
}

case class SetTransition(var transitionId: Int = 0)
  extends EventCmd {
  def sections =
    singleCall("game.setInt", "useTransition", transitionId)
}

case class SetTimer(var minutes: Float = 1, var seconds: Float = 0)
  extends EventCmd {
  def sections = {

    var timeInSeconds = minutes * 60 + seconds

    singleCall("game.setInt", "timer", timeInSeconds)
  }
}

case class ClearTimer()
  extends EventCmd {
  def sections =
    singleCall("game.setInt", "timer", 0)
}

case class GameOver()
  extends EventCmd {
  def sections =
    Array(PlainLines(Array(jsStatement("game.gameOver"), "return;")))
}

case class FadeIn(var duration: Float = 1f)
  extends EventCmd {
  def sections = {
    singleCall("game.setTransition", 0, duration)
  }
}

case class FadeOut(var duration: Float = 0.4f)
  extends EventCmd {
  def sections = {
    singleCall("game.setTransition", 1, duration)
  }
}

case class CallSaveMenu()
  extends EventCmd {
  def sections = {
    singleCall("game.callSaveMenu")
  }
}

case class CallMenu()
  extends EventCmd {
  def sections = {
    singleCall("game.callMenu")
  }
}

case class MoveCamera(var dx: Float = 0, var dy: Float = 0, var async: Boolean = true, var duration: Float = 2f)
  extends EventCmd {
  def sections =
    singleCall("game.moveCamera", dx, dy, async, duration)
}

case class StopSound() extends EventCmd {
  def sections = singleCall("game.stopSound")
}

case class ShowText(
  var lines: Array[String] = Array(),
  var customFace: Option[FaceSpec] = None,
  var useCharacterFace: Boolean = false,
  var characterId: Int = 0) extends EventCmd {
  def processedLines = lines.map { l =>
    // The local variables need to be processed here rather than in the
    // WindowText class, because only the Javascript has access to the local
    // variables.
    val l1 = EventJavascript.toJs(l)
    val l2 = WindowText.javascriptCtrl.replaceAllIn(
      l1, rMatch => {
        """" + %s + """".format(rMatch.group(1))
      })

    RawJs(l2)
  }

  def sections = {
    if (useCharacterFace)
      singleCall("game.showText", processedLines, PrintingTextWindowOptions(
          useCharacterFace = true, characterId = characterId))
    else if (customFace.isDefined)
      singleCall("game.showText", processedLines, PrintingTextWindowOptions(
          useCustomFace = true, faceset = customFace.get.faceset,
          faceX = customFace.get.faceX, faceY = customFace.get.faceY))
    else
      singleCall("game.showText", processedLines)
  }
}