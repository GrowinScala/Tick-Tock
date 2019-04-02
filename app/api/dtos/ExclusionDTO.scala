package api.dtos

import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.json.{ Json, OFormat }

case class ExclusionDTO(
  exclusionId: String,
  taskId: String,
  exclusionDate: Option[Date] = None,
  day: Option[Int] = None,
  dayOfWeek: Option[Int] = None,
  dayType: Option[DayType] = None,
  month: Option[Int] = None,
  year: Option[Int] = None,
  criteria: Option[Criteria] = None)

object ExclusionDTO {

  /**
   * Implicit that defines how a TaskDTO is written and read.
   */
  implicit val exclusionFormat: OFormat[ExclusionDTO] = Json.format[ExclusionDTO]
}
