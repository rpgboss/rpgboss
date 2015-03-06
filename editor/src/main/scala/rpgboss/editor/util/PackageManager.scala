package rpgboss.editor.util

import java.io._

import org.apache.commons.io.FileUtils
import com.google.common.io.Files
import rpgboss.model.Project
import rpgboss.save.SaveFile
import rpgboss.util.ProjectCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import rpgboss.editor.Internationalized._
import rpgboss.lib.Utils
import scala.io.Source

import sys.process._
import java.net.URL

import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.core.ZipFile

object PackageType {
  val ANIMATION = "1"
  val BATTLE_BACKGROUND = "2"
  val BATTLER = "3"
  val MUSIC = "4"
  val PICTURE = "5"
  val SCRIPT = "6"
  val SOUND = "7"
  val SPRITESET = "8"
  val TILESET = "9"
  val WINDOWSKIN = "10"
  val PROJECT = "11"
  val TITLESCREEN = "12"
}

object PackageManager {

	def fileDownloader(url: String, filename: String) = {
	    new URL(url) #> new File(filename) !!
	}

	def downloadPackage(packageid: String, projectPath:String, host:String) = {
	  try {
	    this.fileDownloader(host+"/api/v1/downloadpackage/"+packageid,projectPath+"/"+packageid+".zip")
	  } catch {
	    case e: java.io.IOException => "error occured"
	  }
	}

	def unZipPackage(folder:String, source: String,projectPath:String) = {
		var file = new File(projectPath+"/"+folder+"/as")
		file.mkdirs()
		var zipFile = new ZipFile(source);
		zipFile.extractAll(projectPath+"/"+folder+"/as")

		if(new File(source).exists) {
			new File(source).delete()
		}
	}
  
  def update(packagetype:String, packageId:String, packageName:String, projectPath:String, host:String) = {

  }

  def install(packagetype:String, packageId:String, packageName:String, projectPath:String, host:String) = {

  	println("Install package " + packageName)

  	this.downloadPackage(packageId, projectPath, host)

  	var source = projectPath+"/"+packageId+".zip"

  	packagetype match {
  		case PackageType.ANIMATION =>
  		  var file = new File(projectPath+"/animation/as")
  			file.mkdirs()
  			unZipPackage("animation", source, projectPath)

  		case PackageType.BATTLE_BACKGROUND =>
  		  var file = new File(projectPath+"/battlerbg/as")
  			file.mkdirs()
  			unZipPackage("battlerbg", source, projectPath)

  		case PackageType.BATTLER =>
  		  var file = new File(projectPath+"/battler/as")
  			file.mkdirs()
  			unZipPackage("battler", source, projectPath)

  		case PackageType.MUSIC =>
  		  var file = new File(projectPath+"/music/as")
  			file.mkdirs()
  			unZipPackage("music", source, projectPath)

  		case PackageType.PICTURE =>
  	  	var file = new File(projectPath+"/picture/as")
  			file.mkdirs()
  			unZipPackage("picture", source, projectPath)

  		case PackageType.SCRIPT =>
  			var file = new File(projectPath+"/script/as")
  			file.mkdirs()
  			unZipPackage("script", source, projectPath)

  		case PackageType.SOUND =>
  			var file = new File(projectPath+"/sound/as")
  			file.mkdirs()
  			unZipPackage("sound", source, projectPath)

  		case PackageType.SPRITESET =>
  			var file = new File(projectPath+"/spriteset/as")
  			file.mkdirs()
  			unZipPackage("spriteset", source, projectPath)

  		case PackageType.TILESET =>
  		  var file = new File(projectPath+"/tileset/as")
  			file.mkdirs()
  			unZipPackage("tileset", source, projectPath)

  		case PackageType.WINDOWSKIN =>
  			var file = new File(projectPath+"/windowskin/as")
  			file.mkdirs()
  			unZipPackage("picture", source, projectPath)

  		case PackageType.PROJECT =>

  		case PackageType.TITLESCREEN =>
  			unZipPackage("picture", source, projectPath)

  			

  	}

  }

}