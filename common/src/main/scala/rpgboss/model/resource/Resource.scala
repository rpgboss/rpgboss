package rpgboss.model.resource

import rpgboss.lib._
import scala.collection.JavaConversions._
import java.io._
import rpgboss.model.Project
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.Gdx
import com.google.common.io.CharStreams
import com.google.common.io.Files

trait Resource[T, MT <: AnyRef] extends LazyLogging {
  def name: String
  def metadata: MT
  def meta: MetaResource[T, MT]
  def proj: Project
  def rcType: String = meta.rcType

  def rcTypeDir = new File(proj.rcDir, meta.rcType)

  /**
   * Can return null if it can't be read in either the project or classpath
   */
  def newDataStream: InputStream = {
    val fileInProject = new File(rcTypeDir, name)
    if (fileInProject.isFile() && fileInProject.canRead()) {
      new FileInputStream(fileInProject)
    } else {
      getClass.getClassLoader().getResourceAsStream(
        "%s/%s/%s".format(ResourceConstants.defaultRcDir, meta.rcType, name))
    }
  }

  /**
   * Returns null if not actually in the project.
   */
  def fileFromProject() = {
    val fileInProject = new File(rcTypeDir, name)
    if (fileInProject.isFile() && fileInProject.canRead())
      fileInProject
    else
      null
  }

  /**
   * Returns the path as if this were a resource from the classpath.
   */
  def getClasspathPath =
    "%s/%s/%s".format(ResourceConstants.defaultRcDir, meta.rcType, name)

  def getGdxFileHandle: FileHandle = {
    val file = fileFromProject()
    if (file != null) {
      Gdx.files.absolute(file.getAbsolutePath())
    } else {
      Gdx.files.classpath(getClasspathPath)
    }
  }

  def readAsString = {
    val s = CharStreams.toString(new InputStreamReader(newDataStream, "UTF-8"))
    newDataStream.close()
    s
  }

  def writeMetadata(): Boolean =
    JsonUtils.writeModelToJson(meta.metadataPath(proj, name), metadata)
}

trait MetaResource[T, MT] {
  def rcType: String
  def keyExts: Array[String] // extension to search for when listing resources

  def rcDir(proj: Project) = new File(proj.rcDir, rcType)

  def extensionFilter(file: File): Boolean = {
    for (ext <- keyExts) {
      if (file.getName.endsWith(ext))
        return true
    }
    return false
  }

  /**
   * Lists the built-in system resources of this type.
   */
  def listSystemResources() = {
    // Add in-classpath files.
    val classpathItems = collection.mutable.Buffer[String]()
    for (path <- ResourceConstants.defaultRcList) {
      val components = path.split("/")
      if (!components.isEmpty &&
        components.head == rcType) {
        val file = new File(path)
        if (extensionFilter(file)) {
          val pathWithoutRcType: String = components.tail.mkString("/")
          classpathItems.append(pathWithoutRcType)
        }
      }
    }
    classpathItems.sorted.toArray
  }

  /**
   * Lists the custom resources in the project.
   */
  def listCustomResources(proj: Project): Array[String] = {
    // Add in-project files.
    val resourceDir = rcDir(proj)
    if (!resourceDir.exists())
      resourceDir.mkdir()
    if (resourceDir.isFile())
      return Array()

    val projectItems = collection.mutable.Buffer[String]()
    for (rootFile <- resourceDir.listFiles()) {
      if (rootFile.isFile() && extensionFilter(rootFile)) {
        projectItems.append(rootFile.getName())
      } else if (rootFile.isDirectory()) {
        for (subFile <- rootFile.listFiles()) {
          if (subFile.isFile() && extensionFilter(subFile)) {
            projectItems.append(rootFile.getName() + "/" + subFile.getName())
          }
        }
      }
    }

    projectItems.sorted.toArray
  }

  /**
   * Lists files matching the extension filter in the resource directory, as
   * well as direct child subdirectories.
   */
  def list(proj: Project): Array[String] = {
    (listSystemResources() ++ listCustomResources(proj)).toArray
  }

  /**
   * Lists custom and system resources in in a given folder.
   */
  def listResourcesUnderPath(project: Project, folderPath: String) = {
    list(project).filter(_.startsWith(folderPath)).sorted
  }

  def importCustom(proj: Project, source: File) = {
    assert(extensionFilter(source))
    assert(source.isFile() && source.canRead())
    Files.copy(source, new File(rcDir(proj), source.getName()))
  }

  def deleteCustom(proj: Project, name: String) = {
    val f = new File(rcDir(proj), name)
    assert(f.isFile() && f.canWrite())
    f.delete()
  }

  def metadataPathRelative(name: String) =
    name + "." + Resource.metadataSuffix

  def metadataPath(proj: Project, name: String) =
    new File(rcDir(proj), metadataPathRelative(name))

  // Create a new instance with the default metadata
  def defaultInstance(proj: Project, name: String): T

  def apply(proj: Project, name: String, metadata: MT): T

  // Returns default instance in case of failure to retrieve
  def readFromDisk(proj: Project, name: String)(implicit m: Manifest[MT]): T = {
    val metadataFromFileOpt =
      JsonUtils.readModelFromJson[MT](metadataPath(proj, name))(m)

    val metadataOpt = if (metadataFromFileOpt.isDefined) {
      metadataFromFileOpt
    } else {
      JsonUtils.readModelFromJsonInClasspath[MT](
        "%s/%s/%s".format(ResourceConstants.defaultRcDir, rcType,
          metadataPathRelative(name)))(m)
    }

    metadataOpt.map(apply(proj, name, _)).getOrElse(defaultInstance(proj, name))
  }
}

case class ResourceException(msg: String) extends Exception(msg)

object Resource {
  def metadataSuffix = "metadata.json"
  def jsonSuffix = "json"

  val resourceTypes = List(
    AnimationImage,
    Autotile,
    Battler,
    BattleBackground,
    Faceset,
    Iconset,
    Msgfont,
    Music,
    Picture,
    RpgMap,
    Script,
    Sound,
    Spriteset,
    Tileset,
    Windowskin)
}