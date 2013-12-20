package rpgboss.model.resource
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.Gdx
import java.io.File
import com.typesafe.scalalogging.slf4j.Logging

trait RpgGdxAsset[T] extends Logging {
  def dataFile: File
  // Gdx always uses '/' as its path separator for some reason.
  def gdxAbsPath: String = dataFile.getCanonicalPath().replaceAll("\\\\", "/")

  def loadAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): Unit = {
    try {
      assets.load(gdxAbsPath, m.runtimeClass.asInstanceOf[Class[T]])
    } catch {
      case e: Throwable =>
        logger.error("Could not load an asset: " + gdxAbsPath, e)
    }
  }

  def getAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): T = {
    assets.get(gdxAbsPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def unloadAsset(assets: RpgAssetManager): Unit = {
    assets.unload(gdxAbsPath)
  }

  def getHandle() = Gdx.files.absolute(gdxAbsPath)
}