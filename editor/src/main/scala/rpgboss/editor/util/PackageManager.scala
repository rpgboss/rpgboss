package rpgboss.editor.util

import java.io.File
import org.apache.commons.io.FileUtils
import com.google.common.io.Files
import rpgboss.model.Project
import rpgboss.save.SaveFile
import rpgboss.util.ProjectCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import rpgboss.editor.Internationalized._
import rpgboss.lib.Utils

object PackageType {
  def SCRIPT = 0
}

object PackageManager {
  
  def update(packagetype:Int, packageId:Int, packageName:String) = {

  }

  def install(packagetype:Int, packageId:Int, packageName:String) = {

  }

}