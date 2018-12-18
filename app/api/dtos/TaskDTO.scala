package api.dtos

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.japi
import api.validators.Error
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import api.validators.Error._
import api.utils.DateUtils._
import database.repositories.slick.FileRepositoryImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Data transfer object for the scheduled tasks on the service side.
  * @param startDateAndTime Date and time of when the task is executed.
  * @param taskName Name of the file that is executed.
  */
case class TaskDTO(
                    startDateAndTime: Date,
                    fileName: String
                  )

/**
  * Companion object for the TaskDTO
  */
object TaskDTO {

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepositoryImpl(db)

  /**
    * Method that constructs the TaskDTO giving strings as dates and making the date format validation and conversion from string to date.
    * @param startDateAndTime Date and time of when the task is executed in a String format.
    * @param fileName Name of the file that is executed.
    * @return the taskDTO if the date received is valid. Throws an IllegalArgumentException if it's invalid.
    */
  def construct(startDateAndTime: String, fileName: String): TaskDTO = {
    val date = getValidDate(startDateAndTime)
    TaskDTO(date.get, fileName)
  }

  /**
    * Implicit that defines how a TaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val taskReads: Reads[TaskDTO] = (
    (JsPath \ "startDateAndTime").read[String] and
      (JsPath \ "fileName").read[String]
    ) (TaskDTO.construct _)

  /**
    * Implicit that defines how a TaskDTO is written to a JSON format.
    */
  implicit val taskWrites = new Writes[TaskDTO] {
    def writes(st: TaskDTO): JsValue = {
      Json.obj(
        "startDateAndTime" -> st.startDateAndTime,
        "fileName" -> st.fileName,
      )
    }
  }

}
