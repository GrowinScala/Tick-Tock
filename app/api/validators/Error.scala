package api.validators

import play.api.libs.json.{Json, OFormat}
import play.http.DefaultHttpErrorHandler

case class Error( //TODO: Add error code
                errorCode: String,
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
  lazy val param = Some("param")
  lazy val body = Some("body")
  lazy val endpoint = Some("endpoint")

  //---------------------------------------------------------
  //# TASK ERRORS
  //---------------------------------------------------------

  lazy val invalidCreateTaskFormat = Error(
    errorCode = "error-01",
    message ="""
        |The json format is not valid. Json must contain:
        |- the String fields "startDateAndTime", "fileName" and "taskType" in that order.
        |- the remaining fields "periodType"(String), "period"(Int), "endDateAndTime"(String) and "occurrences"(Int) (in that order) only when taskType is "Periodic".
        |- either an "endDateAndTime" or "occurrences" field but not both.
        |- if this is a PUT request, the task introduced must not have a taskId. (the taskId is introduced in the endpoint)""".stripMargin,
    reason = Error.invalid,
    locationType = None,
    location = body
  )

  lazy val invalidUpdateTaskFormat = Error(
    errorCode = "error-02",
    message = """
        |The json format is not valid. Json must have at least 1 defined field to update.
        |If the taskType is updated as "Periodic", either the oldTask or the updated task must contain the required fields for a periodic task to function.
      """.stripMargin,
    reason = Error.invalid,
    locationType = None,
    location = body
  )

  lazy val invalidTaskUUID = Error(
    errorCode = "error-03",
    message = s"taskId is not a valid UUID string.",
    reason = Error.invalid,
    locationType = Some("taskId"),
    location = body
  )

  lazy val invalidStartDateFormat = Error(
    errorCode = "error-04",
    message = s"startDateAndTime has the wrong format. The date format must be yyyy-MM-dd HH:mm:ss",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = body
  )

  lazy val invalidStartDateValue = Error(
    errorCode = "error-05",
    message = s"startDateAndTime is a date and time that is in the past or the values on the date don't make sense. (e.g.: month field has to be a number between 01 and 31)",
    reason = Error.invalid,
    locationType = Some("startDateAndTime"),
    location = body
  )

  lazy val invalidFileName = Error(
    errorCode = "error-06",
    message = s"file with the introduced fileName was not found in the file storage.",
    reason = Error.notFound,
    locationType = Some("fileName"),
    location = body
  )

  lazy val invalidTaskType = Error(
    errorCode = "error-07",
    message ="""
        |taskType has to be either "RunOnce" (for a single run schedule) or "Periodic" (for schedulings where the file is executed multiple times on a pattern).
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("taskType"),
    location = body
  )

  lazy val invalidPeriodType = Error(
    errorCode = "error-08",
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
    location = body
  )

  lazy val invalidPeriod = Error(
    errorCode = "error-09",
    message = """
        |The period field must be a positive Int excluding 0 (or can be blank when taskType isn't set to "Periodic"). It represents the interval between each periodic task execution.
        |(e.g.: periodType = "Hourly" and period = 2 means it repeats every 2 hours)
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("period"),
    location = body
  )

  lazy val invalidEndDateFormat = Error(
    errorCode = "error-10",
    message = "endDateAndTime has the wrong format. The date format must be yyyy-MM-dd HH:mm:ss.",
    reason = Error.invalid,
    locationType = Some("endDateAndTime"),
    location = body
  )

  lazy val invalidEndDateValue = Error(
    errorCode = "error-11",
    message = s"endDateAndTime is a date and time that happens in the past or before startDateAndTime or the values on the date don't make sense. (e.g.: month field has to be a number between 01 and 31)",
    reason = Error.invalid,
    locationType = Some("endDateAndTime"),
    location = body
  )

  lazy val invalidOccurrences = Error(
    errorCode = "error-12",
    message = """
        |The occurrences field must be a positive Int excluding 0. (this field can't be used when taskType isn't set to "Periodic" or when there is already an endDateAndTime field)
      """.stripMargin,
    reason = Error.invalid,
    locationType = Some("occurrences"),
    location = body
  )

  lazy val invalidTimezone = Error(
    errorCode = "error-13",
    message = s"The timezone does not exist. Check the java.util.TimeZone documentation for a list of possible timezones.",
    reason = Error.invalid,
    locationType = Some("timezone"),
    location = body
  )

  lazy val invalidEndpointId = Error(
    errorCode = "error-14",
    message = """
        |The id introduced in the endpoint request does not exist for any task.
      """.stripMargin,
    reason = Error.invalid,
    locationType = None,
    location = endpoint
  )

  lazy val invalidSchedulingFormat = Error(
    errorCode = "error-15",
    message = s"The schedulings format must either only contain a schedulingDate field or any of the other fields without the schedulingDate. (day, dayOfWeek, dayType, month, year, criteria)",
    reason = Error.invalid,
    locationType = Some("schedulings"),
    location = body
  )
  lazy val invalidSchedulingDateValue = Error(
    errorCode = "error-16",
    message = s"schedulingDate must be a date in the future.",
    reason = Error.invalid,
    locationType = Some("schedulings/schedulingDate"),
    location = body
  )

  lazy val invalidSchedulingDayValue = Error(
    errorCode = "error-17",
    message = s"day must be an Int between 1 and 28/29/30/31. (depending if the month was specified and which month it is)",
    reason = Error.invalid,
    locationType = Some("schedulings/day"),
    location = body
  )

  lazy val invalidSchedulingDayOfWeekValue = Error(
    errorCode = "error-18",
    message = s"dayOfWeek must be an Int between 1 and 7. (1-Sun,2-Mon,3-Tue,4-Wed,5-Tue,6-Fri,7-Sat)",
    reason = Error.invalid,
    locationType = Some("schedulings/dayOfWeek"),
    location = body
  )

  lazy val invalidSchedulingDayTypeValue = Error(
    errorCode = "error-19",
    message = s"dayType must be an Int between 0 and 1. (0-Weekday,1-Weekend)",
    reason = Error.invalid,
    locationType = Some("schedulings/dayType"),
    location = body
  )

  lazy val invalidSchedulingMonthValue = Error(
    errorCode = "error-20",
    message = s"month must be an Int between 1 and 12. (1-Jan,2-Feb,3-Mar,4-Apr,5-May,6-Jun,7-Jul,8-Aug,9-Sep,10-Oct,11-Nov,12-Dec)",
    reason = Error.invalid,
    locationType = Some("schedulings/month"),
    location = body
  )

  lazy val invalidSchedulingYearValue = Error(
    errorCode = "error-21",
    message = s"year must be an Int between the current year and a future year.",
    reason = Error.invalid,
    locationType = Some("schedulings/year"),
    location = body
  )

  lazy val invalidSchedulingCriteriaValue = Error(
    errorCode = "error-22",
    message = """criteria must be a String of either "First", "Second", "Third", "Fourth" and "Last".""",
    reason = Error.invalid,
    locationType = Some("schedulings/criteria"),
    location = body
  )

  lazy val invalidExclusionFormat = Error(
    errorCode = "error-23",
    message = s"The exclusion format must either only contain a exclusionDate field or any of the other fields without the exclusionDate. (day, dayOfWeek, dayType, month, year, criteria)",
    reason = Error.invalid,
    locationType = Some("exclusions"),
    location = body
  )

  lazy val invalidExclusionDateValue = Error(
    errorCode = "error-24",
    message = s"exclusionDate must be a date in the future.",
    reason = Error.invalid,
    locationType = Some("exclusions/exclusionDate"),
    location = body
  )
  lazy val invalidExclusionDayValue = Error(
    errorCode = "error-25",
    message = s"day must be an Int between 1 and 28/29/30/31. (depending if the month was specified and which month it is)",
    reason = Error.invalid,
    locationType = Some("exclusions/day"),
    location = body
  )
  lazy val invalidExclusionDayOfWeekValue = Error(
    errorCode = "error-26",
    message = s"dayOfWeek must be an Int between 1 and 7. (1-Sun,2-Mon,3-Tue,4-Wed,5-Tue,6-Fri,7-Sat)",
    reason = Error.invalid,
    locationType = Some("exclusions/dayOfWeek"),
    location = body
  )
  lazy val invalidExclusionDayTypeValue = Error(
    errorCode = "error-27",
    message = s"dayType must be an Int between 0 and 1. (0-Weekday,1-Weekend)",
    reason = Error.invalid,
    locationType = Some("exclusions/dayType"),
    location = body
  )
  lazy val invalidExclusionMonthValue = Error(
    errorCode = "error-28",
    message = s"month must be an Int between 1 and 12. (1-Jan,2-Feb,3-Mar,4-Apr,5-May,6-Jun,7-Jul,8-Aug,9-Sep,10-Oct,11-Nov,12-Dec)",
    reason = Error.invalid,
    locationType = Some("exclusions/month"),
    location = body
  )
  lazy val invalidExclusionYearValue = Error(
    errorCode = "error-29",
    message = s"year must be an Int between the current year and a future year.",
    reason = Error.invalid,
    locationType = Some("exclusions/year"),
    location = body
  )
  lazy val invalidExclusionCriteriaValue = Error(
    errorCode = "error-30",
    message = """criteria must be a String of either "First", "Second", "Third", "Fourth" and "Last".""",
    reason = Error.invalid,
    locationType = Some("exclusions/criteria"),
    location = body
  )

  //---------------------------------------------------------
  //# FILE ERRORS
  //---------------------------------------------------------

  lazy val invalidUploadFormat = Error(
    errorCode = "error-31",
    message = """Request must be a MultipartFormData and have the first parameter be a File with key 'file' and the second parameter being a String with the file name.""",
    reason = Error.invalid,
    locationType = None,
    location = body
  )

  lazy val invalidUploadFileName = Error(
    errorCode = "error-32",
    message = "There's already another file with that fileName",
    reason = Error.invalid,
    locationType = Some("name"),
    location = body
  )

  lazy val invalidFileExtension = Error(
    errorCode = "error-33",
    message = "The uploaded file must be a .jar file.",
    reason = Error.invalid,
    locationType = Some("file"),
    location = body
  )

}
