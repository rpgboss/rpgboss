package rpgboss.model.battle

import rpgboss.model._

class Battle(
  project: Project,
  characterIds: Seq[Int],
  characterLevels: Seq[Int],
  initialCharacterHps: Seq[Int],
  initialCharacterMps: Seq[Int],
  initialCharacterStatus: Seq[Seq[Int]],
  initialCharacterEquip: Seq[Seq[Int]],
  encounter: Encounter) {
  private var time = 0.0f
  
  
}