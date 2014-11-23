package rpgboss.model.event

import rpgboss.model._
import rpgboss.player._
import org.json4s.TypeHints
import org.json4s.ShortTypeHints
import EventCmd._
import EventJavascript._
import rpgboss.player.entity.WindowText

trait EventCmd extends HasScriptConstants {
  def sections: Array[CodeSection]

  /**
   *  Returns a copy of this EventCmd with a new set of inner commands placed
   *  in section |sectionI|.
   */
  def copyWithNewInnerCmds(sectionI: Int,
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
    classOf[EndOfScript],
    classOf[SetLocalVariable],
    classOf[LockPlayerMovement],
    classOf[ModifyParty],
    classOf[AddRemoveItem],
    classOf[AddRemoveGold],
    classOf[ShowText],
    classOf[Teleport],
    classOf[SetEventState],
    classOf[IncrementEventState],
    classOf[MoveEvent],
    classOf[RunJs],
    classOf[StartBattle],
    classOf[SetInt])) + EventRenameHints
}

/**
 * @deprecated  Scripts no longer end with this, but this will stick around to
 *              allow old scripts to still deserialize correctly.
 */
case class EndOfScript() extends EventCmd {
  def sections = Array(PlainLines(Array("")))
}

case class SetLocalVariable(parameter: EventParameter[_]) extends EventCmd {
  def sections = Array(PlainLines(
      Array("var %s = %s;".format(
          parameter.localVariable, parameter.constant))))
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

  override def copyWithNewInnerCmds(sectionI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    assert(sectionI == 1)
    copy(body = newInnerCmds)
  }
}

case class ModifyParty(add: Boolean = true, characterId: Int = 0)
  extends EventCmd {
  def sections = singleCall("game.modifyParty", add, characterId)
}

case class AddRemoveItem(
  var add: Boolean = true,
  itemId: IntParameter = IntParameter(),
  quantity: IntParameter = IntParameter(EventParameterValueType.Constant.id, 1))
  extends EventCmd {
  // Backwards-compatibility constructor
  def this(itemId: Int, add: Boolean, qty: Int) =
    this(add, IntParameter(itemId), IntParameter(qty))

  def qtyDelta =
    RawJs(if (add) quantity.jsString else "%s * -1".format(quantity.jsString))
  def sections = singleCall("game.addRemoveItem", itemId, qtyDelta)

  override def getParameters() = List(itemId, quantity)
}

case class AddRemoveGold(
  var add: Boolean = true,
  quantity: IntParameter = IntParameter())
  extends EventCmd {
  def qtyDelta =
    RawJs(if (add) quantity.jsString else "%s * -1".format(quantity.jsString))
  def sections = singleCall("game.addRemoveGold", qtyDelta)

  override def getParameters() = List(quantity)
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
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

  def sections = singleCall("game.showText", processedLines)
}

case class Teleport(loc: MapLoc, transitionId: Int) extends EventCmd {
  def sections =
    singleCall("game.teleport", loc.map, loc.x, loc.y, transitionId)
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

case class IncrementEventState() extends EventCmd {
  def sections = singleCall("game.incrementEventState", RawJs("event.id()"))
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
    case EntitySpec(which, _, eventIdx)
    if which == WhichEntity.EVENT_ON_MAP.id =>
      singleCall(
        "game.moveEvent", entitySpec.eventId, dx, dy, affixDirection, async)
  }
}

case class StartBattle(encounterId: Int = 0, battleBackground: String = "")
  extends EventCmd {
  def sections = singleCall("game.startBattle", encounterId, battleBackground)
}

case class RunJs(scriptBody: String = "") extends EventCmd {
  def sections = Array(PlainLines(Array(scriptBody.split("\n"): _*)))
}

case class SetInt(key: String, value: Int) extends EventCmd {
  def sections = singleCall("game.setInt", key, value)
}