package rpgboss.model.resource
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.Gdx
import java.io.File
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable

trait RpgGdxAsset[T] extends LazyLogging {
  def rcType: String
  def name: String

  def gdxPath: String = "%s/%s".format(rcType, name)

  var failed = false

  def loadAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): Boolean = {
    try {
      assets.loadedAssets.add(this)
      assets.load(gdxPath, m.runtimeClass.asInstanceOf[Class[T]])
      true
    } catch {
      case e: Throwable =>
        logger.error("Could not load an asset: " + gdxPath, e)
        false
    }
  }

  def isLoaded(assets: RpgAssetManager) = assets.isLoaded(gdxPath)

  def getAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): T = {
    assets.get(gdxPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def dispose(assets: RpgAssetManager) = {
    if (assets.isLoaded(gdxPath))
      assets.unload(gdxPath)

    assets.loadedAssets.remove(this)
  }
}