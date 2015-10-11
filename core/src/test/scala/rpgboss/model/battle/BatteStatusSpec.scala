package rpgboss.model.battle

import rpgboss.UnitSpec
import rpgboss.model.ProjectData
import rpgboss.model.LearnedSkill

class BatteStatusSpec extends UnitSpec {
  "BattleStatus" should "respect learned skills" in {
    val pData = ProjectData("fake-uuid", "fake-title")
    pData.enums.classes.head.learnedSkills = Array(
        LearnedSkill(1, 5),
        LearnedSkill(5, 10),
        LearnedSkill(50, 15))

    val partyParams = PartyParameters(
        characterLevels = Array(20),
        initialCharacterHps = Array(50),
        initialCharacterMps = Array(50),
        characterEquip = Array(Array()),
        initialCharacterTempStatusEffectIds = Array(Array()),
        learnedSkills = Array(Array(12, 13)),
        characterRows = Array(0))
    val playerStatus = BattleStatus.fromCharacter(pData, partyParams, 0, 0)

    playerStatus.knownSkillIds should deepEqual(Array(5, 10, 12, 13))
  }
}