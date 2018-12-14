package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import api.dtos.TaskDTO
import play.api.libs.json._
import api.dtos.TaskDTO._
import api.validators.Error._

/**
  * Object that handles the validation for the received JSON's on the HTTP request controller classes.
  */
object Validator {

  val dateFormatsList: Array[SimpleDateFormat] = Array(
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  )

  /**
    * Method that handles the validation for the received JSON bodies for task scheduling.
    * @param jsValue JsValue that holds the JSON body.
    * @return Returns the TaskDTO if everything went well or a JsArray with errors.
    */
  def taskParsingErrors(jsValue: JsValue): Either[List[Error], TaskDTO] = {
    jsValue.validate[TaskDTO] match {
      case JsSuccess(task, _) => // Parsing successful
        // Checking the values
        val errorList = List(
          ("startDateAndTime", isValidDateValue(task.startDateAndTime)),
          ("taskName", isValidFileName(task.fileName))
        ).filter(item => item._2.isDefined).map(_._2.get)

        if(errorList.isEmpty) Right(task)
        else Left(errorList)

      case JsError(e) => // Parsing failed
        val errorList: List[Error] = Nil
        e.map(s => s._1.toString.replace("/", "")).map(elem => {
          val jsString = JsString(elem)
          jsString match {
            case JsString("startDateAndTime") =>
              Error.invalidDateValue :: errorList
            case JsString("fileName") =>
              Error.invalidFileName :: errorList
            case JsString(_) =>
              Error.invalidJsonStructure :: errorList
          }
        })
        Left(errorList)
      //Left((e.map(s => JsString(s._1.toString.replace("/", ""))).toIndexedSeq))
    }

  }








  /*
  def isValidLength(string: String, maxLength: Int): Boolean = {
    string.length <= maxLength
  }

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

  def isValidFileName(name: String): Boolean = ???*/
}
