package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import api.dtos.TaskDTO
import play.api.libs.json._
import api.dtos.TaskDTO._
import api.validators.ValidationError._

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
  def taskParsingErrors(jsValue: JsValue): Either[TaskDTO, JsArray] = {
    jsValue.validate[TaskDTO] match {
      case JsSuccess(task, _) => // Parsing successful
        // Checking the values
        val errorList = List(
          ("startDateAndTime", isValidDateValue(task.startDateAndTime)),
          ("taskName", isValidFileName(task.taskName))
        ).filter(item => item._2.isDefined).map(_._2.get)

        if(errorList.isEmpty) Left(task)
        else Right(JsArray(errorList.map(s => Json.toJsObject(s)).toIndexedSeq))

      case JsError(e) =>
        Right(JsArray(e.map(s => JsString(s._1.toString.replace("/", ""))).toIndexedSeq))
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
