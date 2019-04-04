package api.dtos

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

import scala.concurrent.ExecutionContext

case class UpdateExclusionDTO(
  exclusionId: Option[String] = None,
  taskId: Option[String] = None,
  exclusionDate: Option[String] = None,
  day: Option[Int] = None,
  dayOfWeek: Option[Int] = None,
  dayType: Option[DayType] = None,
  month: Option[Int] = None,
  year: Option[Int] = None,
  criteria: Option[Criteria] = None)

object UpdateExclusionDTO {

  implicit val ec: ExecutionContext = ExecutionContext.global

  /**
   * Implicit that defines how an UpdateExclusionDTO is read from the JSON request.
   * This implicit is used on the TaskController when Play's "validate" method is called.
   */
  implicit val updateExclusionReads: Reads[UpdateExclusionDTO] = (
    (JsPath \ "exclusionId").readNullable[String] and
    (JsPath \ "taskId").readNullable[String] and
    (JsPath \ "exclusionDate").readNullable[String] and
    (JsPath \ "day").readNullable[Int] and
    (JsPath \ "dayOfWeek").readNullable[Int] and
    (JsPath \ "dayType").readNullable[DayType] and
    (JsPath \ "month").readNullable[Int] and
    (JsPath \ "year").readNullable[Int] and
    (JsPath \ "criteria").readNullable[Criteria])(UpdateExclusionDTO.apply _)

  /**
   * Implicit that defines how an UpdateExclusionDTO is written to a JSON format.
   */
  implicit val updateExclusionFormat: OWrites[UpdateExclusionDTO] = Json.writes[UpdateExclusionDTO]
}