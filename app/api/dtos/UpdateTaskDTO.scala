package api.dtos

import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import play.api.libs.json.{ JsPath, Json, OWrites, Reads }
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext

case class UpdateTaskDTO(
  taskId: Option[String] = None,
  fileName: Option[String] = None,
  taskType: Option[SchedulingType] = None,
  startDateAndTime: Option[String] = None,
  periodType: Option[PeriodType] = None,
  period: Option[Int] = None,
  endDateAndTime: Option[String] = None,
  occurrences: Option[Int] = None,
  timezone: Option[String] = None,
  exclusions: Option[List[UpdateExclusionDTO]] = None,
  schedulings: Option[List[UpdateSchedulingDTO]] = None)

object UpdateTaskDTO {

  implicit val ec = ExecutionContext.global

  /**
   * Implicit that defines how an UpdateTaskDTO is read from the JSON request.
   * This implicit is used on the TaskController when Play's "validate" method is called.
   */
  implicit val updateTaskReads: Reads[UpdateTaskDTO] = (
    (JsPath \ "taskId").readNullable[String] and
    (JsPath \ "fileName").readNullable[String] and
    (JsPath \ "taskType").readNullable[String] and
    (JsPath \ "startDateAndTime").readNullable[String] and
    (JsPath \ "periodType").readNullable[String] and
    (JsPath \ "period").readNullable[Int] and
    (JsPath \ "endDateAndTime").readNullable[String] and
    (JsPath \ "occurrences").readNullable[Int] and
    (JsPath \ "timezone").readNullable[String] and
    (JsPath \ "exclusions").readNullable[List[UpdateExclusionDTO]] and
    (JsPath \ "schedulings").readNullable[List[UpdateSchedulingDTO]])(UpdateTaskDTO.apply _)

  /**
   * Implicit that defines how an UpdateTaskDTO is written to a JSON format.
   */
  implicit val updateTaskFormat: OWrites[UpdateTaskDTO] = Json.writes[UpdateTaskDTO]
}
