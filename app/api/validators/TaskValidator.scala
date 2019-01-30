package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Date, UUID}

import api.dtos.{CreateTaskDTO, FileDTO, TaskDTO}
import api.utils.DateUtils._
import api.validators.Error._
import database.repositories.{FileRepository, FileRepositoryImpl}
import database.utils.DatabaseUtils.DEFAULT_DB
import javax.inject.{Inject, Singleton}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Object that handles the validation for the received JSON's on the HTTP request controller classes.
  */
@Singleton
class TaskValidator @Inject() (implicit val fileRepo: FileRepository){


  implicit val ec = ExecutionContext.global

  //---------------------------------------------------------
  //# TASK VALIDATORS
  //---------------------------------------------------------

  def scheduleValidator(task: CreateTaskDTO): Either[List[Error],TaskDTO] = {
    val startDate = isValidStartDateFormat(task.startDateAndTime)
    val endDate = isValidEndDateFormat(task.endDateAndTime)
    val errorList = List(
      (isValidTask(task), invalidScheduleFormat),
      (startDate.isDefined, invalidStartDateFormat),
      (isValidStartDateValue(startDate), invalidStartDateValue),
      (isValidFileName(task.fileName), invalidFileName),
      (isValidTaskType(task.taskType), invalidTaskType),
      (isValidPeriodType(task.periodType), invalidPeriodType),
      (isValidPeriod(task.period), invalidPeriod),
      (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
      (isValidEndDateValue(startDate, endDate), invalidEndDateValue),
      (isValidOccurrences(task.occurrences), invalidOccurrences)
    ).filter(!_._1)
    if(errorList.isEmpty) Right(TaskDTO(UUID.randomUUID().toString, task.fileName, task.taskType, startDate, task.periodType, task.period, endDate, task.occurrences, task.occurrences))
    else Left(errorList.unzip._2)
  }

  /*/**
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
  }*/

  private def isValidTask(task: CreateTaskDTO): Boolean ={
    task.taskType match{
      case "RunOnce" =>
        task.periodType.isEmpty ||
        task.period.isEmpty ||
        task.endDateAndTime.isEmpty ||
        task.occurrences.isEmpty
      case "Periodic" =>
        task.periodType.isDefined && task.period.isDefined &&
          ((task.endDateAndTime.isDefined && task.occurrences.isEmpty) || (task.endDateAndTime.isEmpty && task.occurrences.isDefined))
      case _ => false
    }
  }

  private def isValidStartDateFormat(startDate: Option[String]): Option[Date] ={
    if(startDate.isDefined) parseDate(startDate.get)
    else None
  }

  /**
    * Checks if the date given is valid, (if it already happened or not)
    * @param date The Date to be checked
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidStartDateValue(startDate: Option[Date]): Boolean = {
    if(startDate.isDefined) startDate.get.after(getCurrentDate)
    else true
  }

  /**
    * Checks if the file with the given fileName exists.
    * @param fileName The fileName to be checked.
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidFileName(fileName: String): Boolean = {
    Await.result(fileRepo.existsCorrespondingFileName(fileName), Duration.Inf)
  }

  private def isValidTaskType(taskType: String): Boolean = {
    taskType.equals("RunOnce") || taskType.equals("Periodic")
  }

  private def isValidPeriodType(periodType: Option[String]): Boolean = {
    periodType.isEmpty ||
    periodType.get.equals("Minutely") ||
    periodType.get.equals("Hourly") ||
    periodType.get.equals("Daily") ||
    periodType.get.equals("Weekly") ||
    periodType.get.equals("Monthly") ||
    periodType.get.equals("Yearly")
  }

  private def isValidPeriod(period: Option[Int]): Boolean ={
    period.isEmpty || period.get > 0
  }

  private def isValidEndDateFormat(endDate: Option[String]): Option[Date] = {
    if(endDate.isDefined) parseDate(endDate.get)
    else None
  }

  private def isValidEndDateValue(startDate: Option[Date], endDate: Option[Date]): Boolean = {
    if(startDate.isDefined && endDate.isDefined) endDate.isEmpty || endDate.get.after(startDate.get)
    else true
  }

  private def isValidOccurrences(occurrences: Option[Int]): Boolean = {
    occurrences.isEmpty || occurrences.get > 0
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
