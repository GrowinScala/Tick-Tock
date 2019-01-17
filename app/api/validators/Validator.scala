package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import api.dtos.{CreateTaskDTO, FileDTO, TaskDTO}
import play.api.libs.json._
import api.dtos.TaskDTO._
import api.services.SchedulingType
import api.validators.Error._
import api.utils.DateUtils._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Object that handles the validation for the received JSON's on the HTTP request controller classes.
  */
object Validator {

  //---------------------------------------------------------
  //# TASK VALIDATORS
  //---------------------------------------------------------

  def scheduleValidator(task: CreateTaskDTO): Option[List[Error]] = {
    val errorList = List(
      //(isValidDateValue(task.startDateAndTime), invalidDateValue),
      (isValidFileName(task.fileName), invalidFileName),
      //(isValidTaskFormat(task), invalidTaskFormat)
    ).filter(!_._1)
    if(errorList.isEmpty) None
    else Some(errorList.unzip._2)
  }

  /**
    * Method that validates a String representing a date and verifies if it follows any
    * of the permitted formats in the dateFormatsList and attempts to parse it accordingly.
    * @param date Date in a string format.
    * @return Returns the parsed date if the received String is valid encapsulated as an Option[Date].
    *         Returns None if not.
    */
  def getValidDate(date: String): Option[Date] = {
    dateFormatsList.flatMap { format =>
      format.setLenient(false)
      Try(Some(format.parse(date))).getOrElse(None)
    }.headOption
  }

  /**
    * Checks if the date given is valid, (if it already happened or not)
    * @param date The Date to be checked
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidDateValue(date: Date): Boolean = {
    date.after(getCurrentDate)
  }

  /**
    * Checks if the given fileName exists.
    * @param fileName The fileName to be checked.
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidFileName(fileName: String): Boolean = {
    Await.result(fileRepo.existsCorrespondingFileName(fileName), 5 seconds)
  }

  /*private def isValidTaskFormat(task: CreateTaskDTO): Boolean = {
    if(task.taskType.isDefined){
      if(task.taskType.get.equals("RunOnce")){

      }
      else if(task.taskType.get.equals("Periodic")){
        if(task.periodType.isDefined){
          task.periodType.get match{
            case "Minutely" =>
            case "Hourly" =>
            case "Daily" =>
            case "Weekly" =>
            case "Monthly" =>
            case "Yearly" =>
          }
        }
      }
      else false
    }
    else{
      task.periodType.isEmpty && task.period.isEmpty && task.endDateAndTime.isEmpty && task.occurrences.isEmpty
    }
  }*/

  /*def uploadValidator(file: FileDTO): Option[List[Error]] = {
    val errorList = List(

    )
    if(errorList.isEmpty) None
    else Some(errorList.unzip._2)
  }*/
//
//  /**
//    * Method that handles the validation for the received JSON bodies for task scheduling.
//    * @param jsValue JsValue that holds the JSON body.
//    * @return Returns the TaskDTO if everything went well or a JsArray with errors.
//    */
//  def taskParsingErrors(jsValue: JsValue): Either[List[Error], TaskDTO] = {
//    jsValue.validate[TaskDTO] match {
//      case JsSuccess(task, _) => // Parsing successful
//        // Checking the values
//        val errorList = List(
//          ("startDateAndTime", isValidDateValue(task.startDateAndTime)),
//          ("taskName", isValidFileName(task.fileName)) //TODO - Validation should not go to database
//        ).filter(item => item._2.isDefined).map(_._2.get)
//        if(errorList.isEmpty) Right(task)
//        else Left(errorList)
//
//      case JsError(e) => // Parsing failed
//        val errorList: List[Error] = Nil
//        e.map(s => s._1.toString.replace("/", "")).map(elem => {
//          val jsString = JsString(elem)
//          jsString match {
//            case JsString("startDateAndTime") =>
//              Error.invalidDateValue :: errorList
//            case JsString("fileName") =>
//              Error.invalidFileName :: errorList
//            case JsString(_) =>
//              Error.invalidJsonStructure :: errorList
//          }
//        })
//        Left(errorList)
//      //Left((e.map(s => JsString(s._1.toString.replace("/", ""))).toIndexedSeq))
//    }
//
//  }
 }
