package validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

object Validator {

  val dateFormatsList: Array[SimpleDateFormat] = Array(
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  )

  def isValidLength(string: String): Boolean = ???

  def isValidDate(date: String): Int = {

    def iter(array: Array[SimpleDateFormat], index: Int): Int = {
      try{
        array.head.setLenient(false)
        val d = array.head.parse(date)
        val s = array.head.format(d)
        index
      }
      catch{
        case _: Throwable =>
          if(array.tail.isEmpty) -1
          else iter(array.tail, index + 1)
      }

    }
    iter(dateFormatsList, 0)
  }

  def isValidFilePath(filepath: String): Boolean = {
    val defaultFS = FileSystems.getDefault()
    val separator = defaultFS.getSeparator()
    val path = defaultFS.getPath(filepath)
    Files.exists(path)
  }

  def isValidFileName(name: String): Boolean = ???
}
