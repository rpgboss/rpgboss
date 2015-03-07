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

import rpgboss.editor._

import scala.collection.JavaConversions._

import org.apache.commons.io.FileUtils

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

	def packageInfo(folder:String, projectVersion: String,projectPath:String,packageId:String):List[String] = {
		var alreadyExisting = "false"
		var canBeUpdated = "false"
		if(new File(projectPath+"/"+folder+"/as/"+packageId).exists) {
			alreadyExisting = "true"
			var version = scala.io.Source.fromFile(projectPath+"/"+folder+"/as/"+packageId+"/version").mkString
			if(version.toDouble < projectVersion.toDouble) {
				canBeUpdated = "true"
			}
		}
		return List(alreadyExisting,canBeUpdated)
	}

	def unZipPackage(folder:String, source: String,projectPath:String,packageId:String) = {
		var file = new File(projectPath+"/"+folder+"/as")
		file.mkdirs()

		if(new File(projectPath+"/"+folder+"/as/"+packageId).exists) {
			try
			{
				FileUtils.deleteDirectory(new File(projectPath+"/"+folder+"/as/"+packageId));
			}
			catch {
				case ioe: IOException =>
				case e: Exception =>
			}
		}

		VisibleConnection.currentSession.getBasicRemote.sendText("command;editor;{\"action\":\"importStatus\",\"value\":\"Import\"}")

		var zipFile = new ZipFile(source);
		zipFile.extractAll(projectPath+"/"+folder+"/as")

		for(file <- (new File(projectPath+"/"+folder+"/as/"+packageId)).listFiles if file.getName endsWith ".zip"){
			var zipFile = new ZipFile(file);
		  zipFile.extractAll(projectPath+"/"+folder+"/as/"+packageId)
		  if(file.exists) {
				file.delete()
			}
		}

		if(new File(source).exists) {
			new File(source).delete()
		}
	}

	def getPackageInfo(packagetype:String, packageId:String, packageVersion:String, projectPath:String, host:String):List[String] = {

		var result:List[String] = List("false","false")

  	packagetype match {
  		case PackageType.ANIMATION =>
  			result = packageInfo("animation", packageVersion, projectPath, packageId)

  		case PackageType.BATTLE_BACKGROUND =>
  			result =packageInfo("battlerbg", packageVersion, projectPath, packageId)

  		case PackageType.BATTLER =>
  			result =packageInfo("battler", packageVersion, projectPath, packageId)

  		case PackageType.MUSIC =>
  			result =packageInfo("music", packageVersion, projectPath, packageId)

  		case PackageType.PICTURE =>
  			result =packageInfo("picture", packageVersion, projectPath, packageId)

  		case PackageType.SCRIPT =>
  			result =packageInfo("script", packageVersion, projectPath, packageId)

  		case PackageType.SOUND =>
  			result =packageInfo("sound", packageVersion, projectPath, packageId)

  		case PackageType.SPRITESET =>
  			result =packageInfo("spriteset", packageVersion, projectPath, packageId)

  		case PackageType.TILESET =>
  			result =packageInfo("tileset", packageVersion, projectPath, packageId)

  		case PackageType.WINDOWSKIN =>
  			result =packageInfo("picture", packageVersion, projectPath, packageId)

  		case PackageType.PROJECT =>

  		case PackageType.TITLESCREEN =>
  			result =packageInfo("picture", packageVersion, projectPath, packageId)
  	}

  	return result

	}

  def install(packagetype:String, packageId:String, packageName:String, projectPath:String, host:String) = {

  	VisibleConnection.currentSession.getBasicRemote.sendText("command;editor;{\"action\":\"importStatus\",\"value\":\"Downloading\"}")

  	this.downloadPackage(packageId, projectPath, host)

  	var source = projectPath+"/"+packageId+".zip"

  	packagetype match {
  		case PackageType.ANIMATION =>
  		  var file = new File(projectPath+"/animation/as")
  			file.mkdirs()
  			unZipPackage("animation", source, projectPath, packageId)

  		case PackageType.BATTLE_BACKGROUND =>
  		  var file = new File(projectPath+"/battlerbg/as")
  			file.mkdirs()
  			unZipPackage("battlerbg", source, projectPath, packageId)

  		case PackageType.BATTLER =>
  		  var file = new File(projectPath+"/battler/as")
  			file.mkdirs()
  			unZipPackage("battler", source, projectPath, packageId)

  		case PackageType.MUSIC =>
  		  var file = new File(projectPath+"/music/as")
  			file.mkdirs()
  			unZipPackage("music", source, projectPath, packageId)

  		case PackageType.PICTURE =>
  	  	var file = new File(projectPath+"/picture/as")
  			file.mkdirs()
  			unZipPackage("picture", source, projectPath, packageId)

  		case PackageType.SCRIPT =>
  			var file = new File(projectPath+"/script/as")
  			file.mkdirs()
  			unZipPackage("script", source, projectPath, packageId)

  		case PackageType.SOUND =>
  			var file = new File(projectPath+"/sound/as")
  			file.mkdirs()
  			unZipPackage("sound", source, projectPath, packageId)

  		case PackageType.SPRITESET =>
  			var file = new File(projectPath+"/spriteset/as")
  			file.mkdirs()
  			unZipPackage("spriteset", source, projectPath, packageId)

  		case PackageType.TILESET =>
  		  var file = new File(projectPath+"/tileset/as")
  			file.mkdirs()
  			unZipPackage("tileset", source, projectPath, packageId)

  		case PackageType.WINDOWSKIN =>
  			var file = new File(projectPath+"/windowskin/as")
  			file.mkdirs()
  			unZipPackage("picture", source, projectPath, packageId)

  		case PackageType.PROJECT =>

  		case PackageType.TITLESCREEN =>
  			var file = new File(projectPath+"/picture/as")
  			file.mkdirs()
  			unZipPackage("picture", source, projectPath, packageId)

  			

  	}

  }

}