package rpgboss.model.resource

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.Gdx
import rpgboss.model.Project
import java.io.File
import com.badlogic.gdx.assets.AssetErrorListener
import com.badlogic.gdx.assets.AssetDescriptor
import com.typesafe.scalalogging.slf4j.LazyLogging

class RpgFileHandleResolver(proj: Project) extends FileHandleResolver {
  def resolve(path: String) = {
    val fileInProject = new File(proj.dir, path)
    if (fileInProject.isFile() && fileInProject.canRead()) {
      Gdx.files.absolute(fileInProject.getAbsolutePath())
    } else {
      Gdx.files.classpath(
        "%s/%s".format(ResourceConstants.defaultRcDir, path))
    }
  }
}

class RpgAssetManager(proj: Project)
  extends AssetManager(new RpgFileHandleResolver(proj))
  with AssetErrorListener
  with LazyLogging {

  val loadedAssets = collection.mutable.HashSet[RpgGdxAsset[_]]()

  setErrorListener(this)

  override def error(asset: AssetDescriptor[_], throwable: Throwable) = {
    logger.error("Could not load an asset: " + asset.file.path())

    for (gdxAsset <- loadedAssets) {
      if (gdxAsset.gdxPath == asset.file.path()) {
        gdxAsset.failed = true
      }
    }
  }
}