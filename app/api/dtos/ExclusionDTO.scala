package api.dtos

import java.time.LocalDate
import java.util.Date

import api.services.Criteria.Criteria
import api.services.DayType.DayType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class ExclusionDTO(
  exclusionId: String,
  taskId: String,
  exclusionDate: Option[LocalDate] = None,
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

  implicit val exclusionReads: Reads[ExclusionDTO] = (
    (JsPath \ "exclusionId").read[String] and
    (JsPath \ "taskId").read[String] and
    (JsPath \ "exclusionDate").readNullable[LocalDate] and
    (JsPath \ "day").readNullable[Int] and
    (JsPath \ "dayOfWeek").readNullable[Int] and
    (JsPath \ "dayType").readNullable[DayType] and
    (JsPath \ "month").readNullable[Int] and
    (JsPath \ "year").readNullable[Int] and
    (JsPath \ "criteria").readNullable[Criteria])(ExclusionDTO.apply _)

  implicit val exclusionWrites = new Writes[ExclusionDTO] {
    def writes(exclusion: ExclusionDTO) = {
      Json.obj("exclusionId" -> exclusion.exclusionId, "taskId" -> exclusion.taskId) ++
        (if (exclusion.exclusionDate.isDefined) Json.obj("exclusionDate" -> exclusion.exclusionDate.get.toString) else Json.obj()) ++
        (if (exclusion.day.isDefined) Json.obj("day" -> exclusion.day) else Json.obj()) ++
        (if (exclusion.dayOfWeek.isDefined) Json.obj("dayOfWeek" -> exclusion.dayOfWeek) else Json.obj()) ++
        (if (exclusion.dayType.isDefined) Json.obj("dayType" -> exclusion.dayType) else Json.obj()) ++
        (if (exclusion.month.isDefined) Json.obj("month" -> exclusion.month) else Json.obj()) ++
        (if (exclusion.year.isDefined) Json.obj("year" -> exclusion.year) else Json.obj()) ++
        (if (exclusion.criteria.isDefined) Json.obj("criteria" -> exclusion.criteria) else Json.obj())
    }
  }
}
