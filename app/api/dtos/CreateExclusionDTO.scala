package api.dtos

import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class CreateExclusionDTO(
                         exclusionDate: Option[String] = None,
                         day: Option[Int] = None,
                         dayOfWeek: Option[Int] = None,
                         dayType: Option[DayType] = None,
                         month: Option[Int] = None,
                         year: Option[Int] = None,
                         criteria: Option[Criteria] = None
                       )

object CreateExclusionDTO{

  /**
    * Implicit that defines how a CreateTaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val createExclusionReads: Reads[CreateExclusionDTO] = (
      (JsPath \ "exclusionDate").readNullable[String] and
      (JsPath \ "day").readNullable[Int] and
      (JsPath \ "dayOfWeek").readNullable[Int] and
      (JsPath \ "dayType").readNullable[DayType] and
      (JsPath \ "month").readNullable[Int] and
      (JsPath \ "year").readNullable[Int] and
      (JsPath \ "criteria").readNullable[Criteria]
    ) (CreateExclusionDTO.apply _)

  /**
    * Implicit that defines how a CreateExclusionDTO is written and read.
    */
  implicit val createExclusionFormat: OWrites[CreateExclusionDTO] = Json.writes[CreateExclusionDTO]
}
