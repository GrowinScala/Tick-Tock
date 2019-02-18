package api.dtos

import java.util.Date

import api.services.CriteriaType.CriteriaType
import api.services.DayType.DayType
import play.api.libs.json.{Json, OFormat}

case class SchedulingDTO (
                      schedulingId: String,
                      taskId: String,
                      startDate: Option[Date] = None,
                      day: Option[Int] = None,
                      dayOfWeek: Option[Int] = None,
                      dayType: Option[DayType] = None,
                      month: Option[Int] = None,
                      year: Option[Int] = None,
                      criteria: Option[CriteriaType] = None
                    )

object SchedulingDTO {

  /**
    * Implicit that defines how a SchedulingDTO is written and read.
    */
  implicit val schedulingFormat: OFormat[SchedulingDTO] = Json.format[SchedulingDTO]
}