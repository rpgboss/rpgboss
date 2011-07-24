package rpgboss.lib 
import java.io._
import scala.io.Source

import org.apache.commons.codec.binary.Base64
  
class FileHelper(file : File) {
  def write(text : String) : Unit = {
    val fw = new FileWriter(file)
    try{ fw.write(text) }
    finally{ fw.close }
  }

  def readAsString : Option[String] = {
    if(file.isFile && file.canRead)
      Some(Source.fromFile(file).mkString)
    else
      None
  }
  
  def deleteAll() : Unit = {
    def deleteFile(dfile : File) : Unit = {
      if(dfile.isDirectory)
        dfile.listFiles.foreach{ f => deleteFile(f) }
      dfile.delete
    }
    deleteFile(file)
  }

  def getBytes = {

    val is = new FileInputStream(file)

    val length = file.length()

    val bytes = new Array[Byte](length.asInstanceOf[Int])

    is.read(bytes)

    is.close()
    bytes
  }

  def saveBytesToFile(bytes: Array[Byte]) = {
    val fos = new FileOutputStream(file)
    fos.write(bytes)
    fos.close()
  }

  def saveBase64To(b64: String) = {
    saveBytesToFile(Base64.decodeBase64(b64))
  }

  // assumes can read
  def base64Contents : String = 
    new String(Base64.encodeBase64(getBytes))
  
  def copyTo(dest: File) = {
    dest.createNewFile()
    val srcChan = (new FileInputStream(file)).getChannel
    val desChan = (new FileOutputStream(dest)).getChannel
    desChan.transferFrom(srcChan, 0, srcChan.size)
    srcChan.close
    desChan.close
  }
  
  // return value: if a new empty directory was created
  def forceMkdirs() = {
    if(file.exists) {
      if(!file.isDirectory) {
        deleteAll()
        file.mkdirs()
        true
      } else false
    } else {
      file.mkdirs()
      true
    }
  }
}

object FileHelper {
  implicit def file2helper(file : File) = new FileHelper(file)
}
