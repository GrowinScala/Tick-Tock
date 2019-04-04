package api.dtos

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class CreateSchedulingDTO(
  schedulingDate: Option[String] = None,
  day: Option[Int] = None,
  dayOfWeek: Option[Int] = None,
  dayType: Option[DayType] = None,
  month: Option[Int] = None,
  year: Option[Int] = None,
  criteria: Option[Criteria] = None)

object CreateSchedulingDTO {

  /**
   * Implicit that defines how a CreateSchedulingDTO is read from the JSON request.
   * This implicit is used on the TaskController when Play's "validate" method is called.
   */
  implicit val createSchedulingReads: Reads[CreateSchedulingDTO] = (
    (JsPath \ "schedulingDate").readNullable[String] and
    (JsPath \ "day").readNullable[Int] and
    (JsPath \ "dayOfWeek").readNullable[Int] and
    (JsPath \ "dayType").readNullable[DayType] and
    (JsPath \ "month").readNullable[Int] and
    (JsPath \ "year").readNullable[Int] and
    (JsPath \ "criteria").readNullable[Criteria])(CreateSchedulingDTO.apply _)

  /**
   * Implicit that defines how a CreateSchedulingDTO is written and read.
   */
  implicit val createSchedulingFormat: OWrites[CreateSchedulingDTO] = Json.writes[CreateSchedulingDTO]
}
