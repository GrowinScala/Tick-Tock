package api.dtos

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}

import database.utils.DatabaseUtils._
import akka.japi
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import api.utils.DateUtils._
import database.repositories.slick.FileRepositoryImpl

import scala.concurrent.{ExecutionContext, Future}

/**
  * Data transfer object for the scheduled tasks on the service side.
  * @param startDateAndTime Date and time of when the task is executed.
  * @param taskName Name of the file that is executed.
  */
case class TaskDTO(
                    taskId: String,
                    startDateAndTime: Date,
                    fileName: String
                  )

/**
  * Companion object for the TaskDTO
  */
object TaskDTO {

  /**
    * Implicit that defines how a TaskDTO is written and read.
    */
  implicit val taskFormat: OFormat[TaskDTO] = Json.format[TaskDTO]

}
