package api.validators

import play.api.libs.json.{Json, OFormat}

case class Error(
                message: String,
                reason: String,
                locationType: Option[String],
                location: Option[String]
                )

object Error {

  implicit val errorsFormat: OFormat[Error] = Json.format[Error]

  //reasons
  lazy val invalid: String = "invalid"
  lazy val required: String = "required"
  lazy val notAllowed: String = "notAllowed"
  lazy val notFound: String = "notFound"
  lazy val forbidden: String = "forbidden"

  //location
  lazy val header = Some("header")
  lazy val param = Some("param")

  lazy val invalidJsonStructure = Error(
    message = """Json must only contain the String fields "startDateAndTime" and "fileName" in that order""",
    reason = Error.invalid,
    locationType = None,
    location = header
  )

  lazy val fileNameNotFound = Error(
    message = s"fileName doesn't exist.",
    reason = Error.notFound,
    locationType = Some("fileName"),
    location = param
  )

  lazy val invalidFileName = Error(
    message = s"fileName is invalid",
    reason = Error.invalid,
    locationType = Some("fileName"),
    location = param
  )

  lazy val invalidDateFormat = Error(
    message = s"startDateAndTime has the wrong format.",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )

  lazy val invalidDateValue = Error(
    message = s"startDateAndTime is a date and time that is in the past.",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )


}
