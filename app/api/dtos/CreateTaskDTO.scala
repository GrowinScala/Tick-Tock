package api.dtos

import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

case class CreateTaskDTO(
  fileName: String,
  taskType: SchedulingType,
  startDateAndTime: Option[String] = None,
  periodType: Option[PeriodType] = None,
  period: Option[Int] = None,
  endDateAndTime: Option[String] = None,
  occurrences: Option[Int] = None,
  timezone: Option[String] = None,
  exclusions: Option[List[CreateExclusionDTO]] = None,
  schedulings: Option[List[CreateSchedulingDTO]] = None)

object CreateTaskDTO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  /**
   * Implicit that defines how a CreateTaskDTO is read from the JSON request.
   * This implicit is used on the TaskController when Play's "validate" method is called.
   */
  implicit val createTaskReads: Reads[CreateTaskDTO] = (
    (JsPath \ "fileName").read[String] and
    (JsPath \ "taskType").read[String] and
    (JsPath \ "startDateAndTime").readNullable[String] and
    (JsPath \ "periodType").readNullable[PeriodType] and
    (JsPath \ "period").readNullable[Int] and
    (JsPath \ "endDateAndTime").readNullable[String] and
    (JsPath \ "occurrences").readNullable[Int] and
    (JsPath \ "timezone").readNullable[String] and
    (JsPath \ "exclusions").readNullable[List[CreateExclusionDTO]] and
    (JsPath \ "schedulings").readNullable[List[CreateSchedulingDTO]])(CreateTaskDTO.apply _)

  /**
   * Implicit that defines how a CreateTaskDTO is written to a JSON format.
   */
  implicit val createTaskFormat: OWrites[CreateTaskDTO] = Json.writes[CreateTaskDTO]
}
