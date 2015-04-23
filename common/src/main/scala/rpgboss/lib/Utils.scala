package rpgboss.lib

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.Locale
import scala.collection.JavaConverters.mapAsScalaMapConverter
import org.json4s.Formats
import org.json4s.native.Serialization
import org.mozilla.javascript.NativeObject
import javax.imageio.ImageIO
import rpgboss.model.RpgMapData
import java.io.Reader
import com.google.common.io.Resources
import com.google.common.io.Files

class FileHelper(file: File) {
  import FileHelper._

  def write(text: String): Unit = {
    val fw = new FileWriter(file)
    try { fw.write(text) }
    finally { fw.close }
  }

  def deleteAll(): Boolean = {
    def deleteFile(dfile: File): Boolean = {
      val subFilesGone =
        if (dfile.isDirectory)
          dfile.listFiles.foldLeft(true)(_ && deleteFile(_))
        else
          true

      subFilesGone && dfile.delete
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

  def writeBytes(bytes: Array[Byte]) = {
    val fos = new FileOutputStream(file)
    fos.write(bytes)
    fos.close()
  }

  def copyTo(dest: File) = {
    dest.createNewFile()
    val srcChan = (new FileInputStream(file)).getChannel
    val desChan = (new FileOutputStream(dest)).getChannel
    desChan.transferFrom(srcChan, 0, srcChan.size)
    srcChan.close
    desChan.close
  }

  // return value: if we can write to resultant directory
  def makeWriteableDir(): Boolean = {
    if (file.exists) {
      if (file.isDirectory)
        return file.canWrite
      else {
        // It's an ordinary file. Delete and recreate
        file.delete() && file.mkdirs() && file.canWrite
      }
    } else {
      file.mkdirs() && file.canWrite
    }
  }

  // True if it exists and is writeable, OR if we make a new one
  def canWriteToFile(): Boolean =
    file.canWrite || file.createNewFile()

  // creates parent directory and file if necessary. ensure writable
  def getFos(): Option[FileOutputStream] =
    if (file.getParentFile().makeWriteableDir() && file.canWriteToFile())
      Some(new FileOutputStream(file))
    else None

  def useWriter[T](useFunc: (Writer) => T): Option[T] = {
    getFos().map(
        new OutputStreamWriter(_, "UTF-8")) map { writer =>
      val retVal = useFunc(writer)
      writer.close()
      retVal
    }
  }

  def getReader(): Option[BufferedReader] =
    if (file.isFile && file.canRead)
      Some(new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "UTF-8")))
    else None
}

object FileHelper {
  implicit def file2helper(file: File): FileHelper = new FileHelper(file)
}

object Utils {
  // does ceil integer division, Number Conversion, Roland Backhouse, 2001
  // http://stackoverflow.com/questions/17944/
  def ceilIntDiv(n: Int, m: Int) = (n - 1) / m + 1
  def ceilFloatDiv(n: Float, m: Int) = ceilIntDiv(n.ceil.toInt, m)

  def clamped(orig: Int, min: Int, max: Int) = {
    assume(max >= min)
    math.min(max, math.max(min, orig))
  }

  def clamped(orig: Float, min: Float, max: Float) = {
    assume(max >= min)
    math.min(max, math.max(min, orig))
  }

  // Modulus that always returns a positive number
  def pmod(x: Int, m: Int) =
    (x % m + m) % m

  def randomChoose[T](items: Array[T], weights: Array[Float]): T = {
    assume(!items.isEmpty)
    assume(!weights.isEmpty)
    assume(items.length == weights.length)
    val total = weights.sum
    val roll = util.Random.nextFloat * total

    var cumProb = 0f
    for ((item, weight) <- items zip weights) {
      cumProb += weight
      if (roll < cumProb)
        return item
    }

    assert(1 == 0)
    return items.head
  }

  def randomWithPercent(chance: Int) = {
    assert(chance >= 0)
    assert(chance <= 100)
    util.Random.nextInt(100) < chance
  }

  def floatToPercent(x: Float) =
    "%d%%".format((x * 100).round)

  def removeFromSeq[T](seq: Seq[T], i: Int) =
    seq.take(i) ++ seq.drop(i + 1)

  def readClasspathImage(path: String) = {
    val resource = getClass.getClassLoader.getResourceAsStream(path)
    if (resource == null)
      throw new RuntimeException("Could not load: " + path)
    ImageIO.read(resource)
  }

  def copyResource(resourcePath: String, targetFile: File) = {
    val source = Resources.asByteSource(Resources.getResource(resourcePath))
    source.copyTo(Files.asByteSink(targetFile))
  }

  // TODO: Look for a more efficient implementation.
  def deepCopy[A <: AnyRef](a: A)(implicit m: reflect.Manifest[A]): A = {
    // So far RpgMapData.formats is our only polymorphic list for type hints
    deepCopyWithFormats(a, RpgMapData.formats)
  }

  private def deepCopyWithFormats[A <: AnyRef](
      a: A, formats: Formats)(implicit m: reflect.Manifest[A]): A = {
    val json = Serialization.write(a)(formats)
    Serialization.read[A](json)(formats, m)
  }

  /**
   * Generates a filename. Uses left padded 6 digits for number.
   */
  def generateFilename(prefix: String, id: Int, ext: String) =
    "%s%06d.%s".formatLocal(Locale.US, prefix, id, ext)
}

object ArrayUtils {
  def resized[T](
    a: Array[T],
    newSize: Int,
    newDefaultInstance: () => T)(implicit m: Manifest[T]): Array[T] = {
    val oldSize = a.size

    if (newSize > oldSize) {
      val padder = Array.fill(newSize - oldSize)(newDefaultInstance())
      (a ++ padder)
    } else if (newSize < oldSize) {
      a.take(newSize)
    } else a
  }

  def normalizedAry[T](
    a: Array[T],
    minElems: Int,
    maxElems: Int,
    newDefaultInstance: () => T)(implicit m: Manifest[T]): Array[T] =
    if (a.size > maxElems)
      resized(a, maxElems, newDefaultInstance)
    else if (a.size < minElems)
      resized(a, minElems, newDefaultInstance)
    else
      a
}

object JsonUtils {
  import FileHelper._

  val defaultFormats = RpgMapData.formats

  def readModelFromJsonInClasspath[T](path: String)(
      implicit m: Manifest[T]): Option[T] = {
    val stream = getClass.getClassLoader().getResourceAsStream(path)
    if (stream == null)
      return None

    val reader = new java.io.InputStreamReader(stream)
    if (reader == null)
      return None

    Some(Serialization.read[T](reader)(defaultFormats, m))
  }

  def readModelFromJsonWithFormats[T](
    file: File, formats: Formats)(implicit m: Manifest[T]): Option[T] = {
    file.getReader().map { reader =>
      Serialization.read[T](reader)(formats, m)
    }
  }

  def readModelFromJson[T](file: File)(implicit m: Manifest[T]) =
    readModelFromJsonWithFormats(file, defaultFormats)(m)

  def writeModelToJson[T <: AnyRef](file: File, model: T): Boolean =
    writeModelToJsonWithFormats(file, model, defaultFormats)

  def writeModelToJsonWithFormats[T <: AnyRef](
    file: File, model: T, formats: Formats): Boolean = {
    file.useWriter { writer =>
      Serialization.writePretty(model, writer)(formats) != null
    } getOrElse false
  }

  def toJsonString[T <: AnyRef](model: T) = {
    Serialization.write(model)(defaultFormats)
  }

  def nativeObjectToCaseClass[T](
    jsObj: NativeObject)(implicit m: Manifest[T]) = {
    import scala.collection.JavaConverters._
    implicit val formats = defaultFormats
    val asScalaMap = jsObj.asScala.toMap
    val json = Serialization.write[Map[_, _]](asScalaMap)

    Serialization.read[T](json)
  }
}

object TweenUtils {
  def tweenAlpha(start: Float, end: Float, current: Float) = {
    assert(start <= current)
    assert(current <= end)
    (current - start) / (end - start)
  }

  def tweenFloat(alpha: Float, startValue: Float, endValue: Float) = {
    assert(alpha >= 0)
    assert(alpha < 1)
    (1 - alpha) * startValue + alpha * endValue
  }

  /**
   * Returns an integer in the interval [startValue, endValue).
   * @param   alpha   Should be in interval [0, 1)
   */
  def tweenInt(alpha: Float, startValue: Int, endValue: Int): Int = {
    assert(alpha >= 0)
    assert(alpha < 1)
    tweenFloat(alpha, startValue, endValue).toInt
  }

  /**
   * Returns an integer in the interval [startValue, endValue]
   */
  def tweenIntInclusive(alpha: Float, startValue: Int, endValue: Int) = {
    tweenInt(alpha, startValue, endValue + 1)
  }
}