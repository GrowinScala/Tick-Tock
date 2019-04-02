package api.dtos

import java.text.SimpleDateFormat
import java.util.{ Date, TimeZone, UUID }

import database.utils.DatabaseUtils._
import akka.japi
import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import database.repositories.FileRepositoryImpl

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Data transfer object for the scheduled tasks on the service side.
 * @param startDateAndTime Date and time of when the task is executed.
 * @param taskName Name of the file that is executed.
 */
case class TaskDTO(
  taskId: String,
  fileName: String,
  taskType: SchedulingType,
  startDateAndTime: Option[Date] = None,
  periodType: Option[PeriodType] = None,
  period: Option[Int] = None,
  endDateAndTime: Option[Date] = None,
  totalOccurrences: Option[Int] = None,
  currentOccurrences: Option[Int] = None,
  timezone: Option[String] = None,
  exclusions: Option[List[ExclusionDTO]] = None,
  schedulings: Option[List[SchedulingDTO]] = None)

/**
 * Companion object for the TaskDTO
 */
object TaskDTO {

  /*implicit val taskWrites: Writes[TaskDTO] = (
      (JsPath \ "taskId").write[String] and
      (JsPath \ "fileName").write[String] and
      (JsPath \ "taskType").write[String] and
      (JsPath \ "startDateAndTime").writeNullable[Date] and
      (JsPath \ "periodType").writeNullable[String] and
      (JsPath \ "period").writeNullable[Int] and
      (JsPath \ "endDateAndTime").writeNullable[Date] and
      (JsPath \ "totalOccurrences").writeNullable[Int] and
      (JsPath \ "currentOccurrences").writeNullable[Int]
    )(unlift(TaskDTO.unapply))*/

  /**
   * Implicit that defines how a TaskDTO is written and read.
   */
  implicit val taskFormat: OFormat[TaskDTO] = Json.format[TaskDTO]
}
