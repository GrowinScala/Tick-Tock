package api.dtos

import java.util.Date

import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
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
  implicit val taskReads: Reads[TaskDTO] = (
    (JsPath \ "taskId").read[String] and
    (JsPath \ "fileName").read[String] and
    (JsPath \ "taskType").read[SchedulingType] and
    (JsPath \ "startDateAndTime").readNullable[Date] and
    (JsPath \ "periodType").readNullable[PeriodType] and
    (JsPath \ "period").readNullable[Int] and
    (JsPath \ "endDateAndTime").readNullable[Date] and
    (JsPath \ "totalOccurrences").readNullable[Int] and
    (JsPath \ "currentOccurrences").readNullable[Int] and
    (JsPath \ "timezone").readNullable[String] and
    (JsPath \ "exclusions").readNullable[List[ExclusionDTO]] and
    (JsPath \ "schedulings").readNullable[List[SchedulingDTO]])(TaskDTO.apply _)

  implicit val taskWrites = new Writes[TaskDTO] {
    def writes(task: TaskDTO) = {
      Json.obj(
        "taskId" -> task.taskId,
        "fileName" -> task.fileName,
        "taskType" -> task.taskType,
        "startDateAndTime" -> task.startDateAndTime.toString,
        "periodType" -> task.periodType,
        "period" -> task.period,
        "endDateAndTime" -> task.endDateAndTime.toString,
        "totalOccurrences" -> task.totalOccurrences,
        "currentOccurrences" -> task.currentOccurrences,
        "timezone" -> task.timezone,
        "exclusions" -> task.exclusions,
        "schedulings" -> task.schedulings)
    }


  }
}
