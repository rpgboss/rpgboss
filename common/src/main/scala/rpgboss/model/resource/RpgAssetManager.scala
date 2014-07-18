package rpgboss.model.resource

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.Gdx
import rpgboss.model.Project
import java.io.File

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
  extends AssetManager(new RpgFileHandleResolver(proj)) {
}