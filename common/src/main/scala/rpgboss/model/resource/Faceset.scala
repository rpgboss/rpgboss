package rpgboss.model.resource

import rpgboss.model.Project

case class FacesetMetadata()

case class Faceset(proj: Project,
                   name: String,
                   metadata: FacesetMetadata)
  extends TiledImageResource[Faceset, FacesetMetadata] {
  def meta = Faceset

  lazy val tileH = img.getHeight / 2
  lazy val tileW = img.getWidth / 4
}

object Faceset extends MetaResource[Faceset, FacesetMetadata] {
  def rcType = "faceset"
  def keyExts = Array("png")

  def defaultInstance(proj: Project, name: String) =
    Faceset(proj, name, FacesetMetadata())
}

