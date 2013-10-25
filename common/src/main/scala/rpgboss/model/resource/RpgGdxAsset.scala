package rpgboss.model.resource
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.Gdx
import java.io.File

trait RpgGdxAsset[T] {
  def dataFile: File
  // Gdx always uses '/' as its path separator for some reason.
  def gdxAbsPath: String = dataFile.getCanonicalPath().replaceAll("\\\\", "/")

  def loadAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): Unit = {
    assets.load(gdxAbsPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def getAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): T = {
    assets.get(gdxAbsPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def unloadAsset(assets: RpgAssetManager): Unit = {
    assets.unload(gdxAbsPath)
  }
  
  def getHandle() = Gdx.files.absolute(gdxAbsPath)
}