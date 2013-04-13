package rpgboss.model.resource
import com.badlogic.gdx.assets.AssetManager

trait RpgGdxAsset[T] {
  def absPath: String

  def loadAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): Unit = {
    assets.load(absPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def getAsset(assets: RpgAssetManager)(implicit m: Manifest[T]): T = {
    assets.get(absPath, m.runtimeClass.asInstanceOf[Class[T]])
  }

  def unloadAsset(assets: RpgAssetManager): Unit = {
    assets.unload(absPath)
  }
}