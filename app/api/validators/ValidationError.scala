package api.validators

import play.api.libs.json.{Json, OFormat}

case class ValidationError(
                message: String,
                reason: String,
                locationType: Option[String],
                location: Option[String]
                )

object ValidationError {

  implicit val errorsFormat: OFormat[ValidationError] = Json.format[ValidationError]

  //reasons
  lazy val invalid: String = "invalid"
  lazy val required: String = "required"
  lazy val notAllowed: String = "notAllowed"
  lazy val notFound: String = "notFound"
  lazy val forbidden: String = "forbidden"

  //location
  lazy val header = Some("header")
  lazy val param = Some("param")

  lazy val fileNameNotFound = ValidationError(
    message = s"taskName doesn't exist.",
    reason = ValidationError.notFound,
    locationType = Some("taskName"),
    location = param
  )

  lazy val invalidDateFormat = ValidationError(
    message = s"startDateAndTime has the wrong format.",
    reason = ValidationError.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )

  lazy val invalidDateValue = ValidationError(
    message = s"startDateAndTime is a date and time that is in the past.",
    reason = ValidationError.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )
}
