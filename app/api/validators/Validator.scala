package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date
import api.dtos.TaskDTO
import play.api.libs.json._
import api.dtos.TaskDTO._
import api.validators.Error._
import api.utils.DateUtils._

import scala.concurrent.ExecutionContext

/**
  * Object that handles the validation for the received JSON's on the HTTP request controller classes.
  */
object Validator {

  def taskValidator(task: TaskDTO): Option[List[Error]] = {
    val errorList = List(
      (isValidDateValue(task.startDateAndTime), invalidDateValue),
      (isValidFileName(task.fileName), invalidFileName)
    )
    if(errorList.forall(elem => elem._1)) None
    else {
      Some(errorList.filter(elem => elem._1).unzip._2)
    }
  }
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
