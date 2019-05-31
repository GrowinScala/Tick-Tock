package api.dtos

import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class SchedulingDTO(
  schedulingId: String,
  taskId: String,
  schedulingDate: Option[Date] = None,
  day: Option[Int] = None,
  dayOfWeek: Option[Int] = None,
  dayType: Option[DayType] = None,
  month: Option[Int] = None,
  year: Option[Int] = None,
  criteria: Option[Criteria] = None)

object SchedulingDTO {

  /**
   * Implicit that defines how a SchedulingDTO is written and read.
   */

  implicit val schedulingReads: Reads[SchedulingDTO] = (
    (JsPath \ "schedulingId").read[String] and
    (JsPath \ "taskId").read[String] and
    (JsPath \ "schedulingDate").readNullable[Date] and
    (JsPath \ "day").readNullable[Int] and
    (JsPath \ "dayOfWeek").readNullable[Int] and
    (JsPath \ "dayType").readNullable[DayType] and
    (JsPath \ "month").readNullable[Int] and
    (JsPath \ "year").readNullable[Int] and
    (JsPath \ "criteria").readNullable[Criteria])(SchedulingDTO.apply _)

  implicit val schedulingWrites = new Writes[SchedulingDTO] {
    def writes(scheduling: SchedulingDTO) = {
      Json.obj("schedulingId" -> scheduling.schedulingId, "taskId" -> scheduling.taskId) ++
        (if (scheduling.schedulingDate.isDefined) Json.obj("schedulingDate" -> scheduling.schedulingDate.get.toString) else Json.obj()) ++
        (if (scheduling.day.isDefined) Json.obj("day" -> scheduling.day) else Json.obj()) ++
        (if (scheduling.dayOfWeek.isDefined) Json.obj("dayOfWeek" -> scheduling.dayOfWeek) else Json.obj()) ++
        (if (scheduling.dayType.isDefined) Json.obj("dayType" -> scheduling.dayType) else Json.obj()) ++
        (if (scheduling.month.isDefined) Json.obj("month" -> scheduling.month) else Json.obj()) ++
        (if (scheduling.year.isDefined) Json.obj("year" -> scheduling.year) else Json.obj()) ++
        (if (scheduling.criteria.isDefined) Json.obj("criteria" -> scheduling.criteria) else Json.obj())
    }
  }
}
