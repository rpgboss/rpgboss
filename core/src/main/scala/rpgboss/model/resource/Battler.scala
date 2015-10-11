package rpgboss.model.resource

import rpgboss.model._

case class BattlerMetadata()

case class Battler(proj: Project, name: String,
                   metadata: BattlerMetadata)
  extends ImageResource[Battler, BattlerMetadata] {
  def meta = Battler

}

object Battler extends MetaResource[Battler, BattlerMetadata] {
  def rcType = "battler"
  def keyExts = Array("png", "jpg", "jpeg")

  def defaultInstance(proj: Project, name: String) =
    Battler(proj, name, BattlerMetadata())
}
