package api.dtos

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

case class UpdateSchedulingDTO(
  schedulingId: Option[String] = None,
  taskId: Option[String] = None,
  schedulingDate: Option[String] = None,
  day: Option[Int] = None,
  dayOfWeek: Option[Int] = None,
  dayType: Option[DayType] = None,
  month: Option[Int] = None,
  year: Option[Int] = None,
  criteria: Option[Criteria] = None)

object UpdateSchedulingDTO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  /**
   * Implicit that defines how an UpdateSchedulingDTO is read from the JSON request.
   * This implicit is used on the TaskController when Play's "validate" method is called.
   */
  implicit val updateSchedulingReads: Reads[UpdateSchedulingDTO] = (
    (JsPath \ "exclusionId").readNullable[String] and
    (JsPath \ "taskId").readNullable[String] and
    (JsPath \ "exclusionDate").readNullable[String] and
    (JsPath \ "day").readNullable[Int] and
    (JsPath \ "dayOfWeek").readNullable[Int] and
    (JsPath \ "dayType").readNullable[DayType] and
    (JsPath \ "month").readNullable[Int] and
    (JsPath \ "year").readNullable[Int] and
    (JsPath \ "criteria").readNullable[Criteria])(UpdateSchedulingDTO.apply _)

  /**
   * Implicit that defines how an UpdateSchedulingDTO is written to a JSON format.
   */
  implicit val updateSchedulingFormat: OWrites[UpdateSchedulingDTO] = Json.writes[UpdateSchedulingDTO]
}