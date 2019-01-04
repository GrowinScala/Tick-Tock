package api.dtos

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}

import database.utils.DatabaseUtils._
import akka.japi
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import api.utils.DateUtils._
import database.repositories.slick.FileRepositoryImpl

import scala.concurrent.{ExecutionContext, Future}

case class CreateTaskDTO(
                     startDateAndTime: Date,
                     fileName: String
                   )

object CreateTaskDTO {

  implicit val ec = ExecutionContext.global
  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)

  /**
    * Method that constructs the TaskDTO giving strings as dates and making the date format validation and conversion from string to date.
    * @param startDateAndTime Date and time of when the task is executed in a String format.
    * @param fileName Name of the file that is executed.
    * @return the taskDTO if the date received is valid. Throws an IllegalArgumentException if it's invalid.
    */
  def construct(startDateAndTime: String, fileName: String): CreateTaskDTO = {
    //val date = getValidDate(startDateAndTime).get
    val date = stringToDateFormat(startDateAndTime, "yyyy-MM-dd HH:mm:ss")
    CreateTaskDTO.apply(date, fileName)
  }

  /**
    * Implicit that defines how a CreateTaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val createTaskReads: Reads[CreateTaskDTO] = (
    (JsPath \ "startDateAndTime").read[String] and
      (JsPath \ "fileName").read[String]
    ) (CreateTaskDTO.construct _)

  /**
    * Implicit that defines how a CreateTaskDTO is written to a JSON format.
    */
  implicit val createTaskFormat: OWrites[CreateTaskDTO] = Json.writes[CreateTaskDTO]
}
