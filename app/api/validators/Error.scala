package api.validators

import play.api.libs.json.{Json, OFormat}
import play.http.DefaultHttpErrorHandler

case class Error(
                message: String,
                reason: String,
                locationType: Option[String],
                location: Option[String]
                )

object Error{

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

  //---------------------------------------------------------
  //# TASK ERRORS
  //---------------------------------------------------------

  lazy val invalidScheduleFormat = Error(
    message ="""
        |The json format is not valid. Json must contain:
        |- the String fields "startDateAndTime", "fileName" and "taskType" in that order.
        |- the remaining fields "periodType"(String), "period"(Int), "endDateAndTime"(String) and "occurrences"(Int) (in that order) only when taskType is "Periodic".
        |- either an "endDateAndTime" or "occurrences" field but not both.""".stripMargin,
    reason = Error.invalid,
    locationType = None,
    location = header
  )

  lazy val invalidStartDateFormat = Error(
    message = s"startDateAndTime has the wrong format. The date format must be yyyy-MM-dd HH:mm:ss",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )

  lazy val invalidStartDateValue = Error(
    message = s"startDateAndTime is a date and time that is in the past or the values on the date don't make sense. (e.g.: month field has to be a number between 01 and 31)",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = param
  )

  lazy val invalidFileName = Error(
    message = s"file with the introduced fileName doesn't exist in the file storage.",
    reason = Error.notFound,
    locationType = Some("fileName"),
    location = param
  )

  lazy val invalidTaskType = Error(
    message ="""
        |taskType has to be either "RunOnce" (for a single run schedule) or "Periodic" (for schedulings where the file is executed multiple times on a pattern).
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("taskType"),
    location = param
  )

  lazy val invalidPeriodType = Error(
    message = """
        |periodType has to be one of these options (or can be blank when taskType isn't set to "Periodic"):
        |- "Minutely" for tasks that repeat on a minute basis.
        |- "Hourly" for tasks that repeat on an hourly basis.
        |- "Daily" for tasks that repeat on a daily basis.
        |- "Weekly" for tasks that repeat on a weekly basis.
        |- "Monthly" for tasks that repeat on a monthly basis.
        |- "Yearly" for tasks that repeat on a yearly basis.
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("periodType"),
    location = param
  )

  lazy val invalidPeriod = Error(
    message = """
        |The period field must be a positive Int excluding 0 (or can be blank when taskType isn't set to "Periodic"). It represents the interval between each periodic task execution.
        |(e.g.: periodType = "Hourly" and period = 2 means it repeats every 2 hours)
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("period"),
    location = param
  )

  /*lazy val invalidEndDateFormat = Error(
    message = """
        |endDateAndTime has the wrong format. (this field can't be used when taskType isn't set to "Periodic" or when there is already an occurrences field)
        |Here are the supported formats:
        |- yyyy-MM-dd HH:mm:ss
        |- dd-MM-yyyy HH:mm:ss
        |- yyyy/MM/dd HH:mm:ss
        |- dd/MM/yyyy HH:mm:ss
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("endDateAndTime"),
    location = param
  )*/

  lazy val invalidEndDateFormat = Error(
    message = "endDateAndTime has the wrong format. The date format must be yyyy-MM-dd HH:mm:ss.",
    reason = Error.invalid,
    locationType = Some("endDateAndTime"),
    location = param
  )

  lazy val invalidEndDateValue = Error(
    message = s"endDateAndTime is a date and time that happens in the past or before startDateAndTime or the values on the date don't make sense. (e.g.: month field has to be a number between 01 and 31)",
    reason = Error.invalid,
    locationType = Some("endDateAndTime"),
    location = param
  )

  lazy val invalidOccurrences = Error(
    message = """
        |The occurrences field must be a positive Int excluding 0. (this field can't be used when taskType isn't set to "Periodic" or when there is already an endDateAndTime field)
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("occurrences"),
    location = param
  )

  //---------------------------------------------------------
  //# FILE ERRORS
  //---------------------------------------------------------

  lazy val invalidUploadFormat = Error(
    message = """Request must be a MultipartFormData and have the first parameter be a File with key 'file' and the second parameter being a String with the file name.""",
    reason = Error.invalid,
    locationType = None,
    location = header
  )

  lazy val invalidUploadFileName = Error(
    message = "There's already another file with that fileName",
    reason = Error.invalid,
    locationType = Some("name"),
    location = param
  )

  lazy val invalidFileExtension = Error(
    message = "The uploaded file must be a .jar file.",
    reason = Error.invalid,
    locationType = Some("file"),
    location = param
  )








}
