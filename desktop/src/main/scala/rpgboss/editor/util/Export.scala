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

object Zip {
  def zipFolder(folder: File, outputZip: File, needUnixPerm: Boolean) = {
    val out = new ZipArchiveOutputStream(outputZip)
    addFolderToZip(folder.getParentFile(), folder, out, needUnixPerm)
    out.close()
  }

  private def addFolderToZip(rootFolder: File, folder: File,
                             out: ZipArchiveOutputStream,
                             needUnixPerm: Boolean): Unit = {
    for (f <- folder.listFiles()) {
      if (f.isDirectory()) {
        addFolderToZip(rootFolder, f, out, needUnixPerm)
      } else {
        val entry = new ZipArchiveEntry(f, f.getCanonicalPath().drop(
            rootFolder.getCanonicalPath().length + 1))

        if (needUnixPerm && f.canExecute())
          entry.setUnixMode(493)  // 755 in octal

        out.putArchiveEntry(entry)
        FileUtils.copyFile(f, out)
        out.closeArchiveEntry()
      }
    }
  }
}

object Export {
  /**
   * Cleans unneeded files from the project directory.
   */
  def cleanGamedata(dir: File) = {
    for (i <- 0 until 50) {
      SaveFile.file(dir, i).delete()
    }

    FileUtils.deleteDirectory(new File(dir, getMessage("Export")))
  }

  def export(project: Project, executableJarFile: File): Unit = {
    val exportDir = new File(project.dir, "export")
    exportDir.mkdir()

    if (!exportDir.isDirectory())
      throw new RuntimeException("Cannot write to export directory")

    val gamedataDir = Files.createTempDir()
    FileUtils.copyDirectory(project.dir, gamedataDir)
    cleanGamedata(gamedataDir)

    val libraryJarName = "rpgboss-library.jar"

    def generatePackage(platformName: String, launchSource: String,
                        launchTargetFilename: String,
                        needUnixPerms: Boolean) = {
      val tempDir = Files.createTempDir()
      val packageName = "%s-%s".format(project.dir.getName(), platformName)
      val dir = new File(tempDir, packageName)
      dir.mkdir()

      val launchFile = new File(dir, launchTargetFilename)

      FileUtils.copyFile(executableJarFile, new File(dir, libraryJarName))
      FileUtils.copyDirectory(gamedataDir, new File(dir, "gamedata"))
      Utils.copyResource(launchSource, launchFile)
      launchFile.setExecutable(true, false)

      val zipName = packageName + ".zip"
      Zip.zipFolder(dir, new File(exportDir, zipName), needUnixPerms)
    }

    generatePackage("linux-mac", "exportresources/linux/launch.sh", "rpgboss", true)
    generatePackage("windows", "exportresources/win/rpgboss-player.exe",
        "rpgboss.exe", false)
  }
}