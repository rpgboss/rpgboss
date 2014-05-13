package rpgboss.model.resource

import rpgboss.model._

case class BattleBackgroundMetadata()

case class BattleBackground(proj: Project, name: String,
                            metadata: BattleBackgroundMetadata)
  extends ImageResource[BattleBackground, BattleBackgroundMetadata] {
  def meta = BattleBackground

}

object BattleBackground 
  extends MetaResource[BattleBackground, BattleBackgroundMetadata] {
  def rcType = "battlebg"
  def keyExts = Array("png", "jpg", "jpeg")

  def defaultInstance(proj: Project, name: String) =
    BattleBackground(proj, name, BattleBackgroundMetadata())
}
