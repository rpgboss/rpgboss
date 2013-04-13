package rpgboss.model.resource

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.Gdx
import rpgboss.model.Project

class AbsoluteFileHandleResolver extends FileHandleResolver {
  def resolve(filename: String) = Gdx.files.absolute(filename)
}

class RpgAssetManager(proj: Project)
  extends AssetManager(new AbsoluteFileHandleResolver()) {

}