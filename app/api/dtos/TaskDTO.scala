package api.dtos

import java.util.Date

import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import play.api.libs.json._

/**
 * Data transfer object for the scheduled tasks on the service side.
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

  /**
   * Implicit that defines how a TaskDTO is written and read.
   */
  implicit val taskFormat: OFormat[TaskDTO] = Json.format[TaskDTO]
}
