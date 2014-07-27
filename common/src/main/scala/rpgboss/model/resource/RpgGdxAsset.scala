package rpgboss.model.resource
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.Gdx
import java.io.File
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.files.FileHandle

trait RpgGdxAsset[T] extends LazyLogging {
  def rcType: String
  def name: String

  def gdxPath: String = "%s/%s".format(rcType, name)

  def loadAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): Unit = {
    try {
      assets.load(gdxPath, m.runtimeClass.asInstanceOf[Class[T]])
    } catch {
      case e: Throwable =>
        logger.error("Could not load an asset: " + gdxPath, e)
    }
  }

  def isLoaded(assets: RpgAssetManager) = assets.isLoaded(gdxPath)

  def getAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): T = {
    assets.get(gdxPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def unloadAsset(assets: RpgAssetManager): Unit = {
    assets.unload(gdxPath)
  }
}