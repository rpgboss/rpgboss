package rpgboss.model.resource

import rpgboss.model._

case class PictureMetadata()

case class Picture(proj: Project, name: String,
                   metadata: PictureMetadata)
  extends ImageResource[Picture, PictureMetadata] {
  def meta = Picture

}

object Picture extends MetaResource[Picture, PictureMetadata] {
  def rcType = "picture"
  def keyExts = Array("png", "jpg", "jpeg")

  def defaultInstance(proj: Project, name: String) =
    Picture(proj, name, PictureMetadata())
}
