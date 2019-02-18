package api.dtos

import api.services.CriteriaType.CriteriaType
import api.services.DayType.DayType
import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}

case class CreateSchedulingDTO (
                            startDate: Option[String] = None,
                            day: Option[Int] = None,
                            dayOfWeek: Option[Int] = None,
                            dayType: Option[DayType] = None,
                            month: Option[Int] = None,
                            year: Option[Int] = None,
                            criteria: Option[CriteriaType] = None,

                          )

object CreateSchedulingDTO {

  implicit val ec = ExecutionContext.global

  /**
    * Implicit that defines how a CreateTaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val createSchedulingReads: Reads[CreateSchedulingDTO] = (
      (JsPath \ "startDate").readNullable[String] and
      (JsPath \ "day").readNullable[Int] and
      (JsPath \ "dayOfWeek").readNullable[Int] and
      (JsPath \ "dayType").readNullable[DayType] and
      (JsPath \ "month").readNullable[Int] and
      (JsPath \ "year").readNullable[Int] and
      (JsPath \ "criteria").readNullable[CriteriaType]
    ) (CreateSchedulingDTO.apply _)

  /**
    * Implicit that defines how a CreateTaskDTO is written to a JSON format.
    */
  implicit val createSchedulingWrites: OWrites[CreateSchedulingDTO] = Json.writes[CreateSchedulingDTO]

}

