package api.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.{ LocalDate, ZoneId }
import java.util.concurrent.TimeUnit
import java.util.{ Calendar, Date, TimeZone }

import api.services.DayType
import api.services.DayType.DayType

import scala.concurrent.duration.Duration
import scala.util.Try

object DateUtils {

  //---------------------------------------------------------
  //# DATE FORMATS
  //---------------------------------------------------------

  /**
   * List of permitted date formats for the startDateAndTime field.
   * It is used for validation for received date strings through the HTTP request.
   */
  final val dateFormatsList: List[SimpleDateFormat] = List(
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"))

  final val localDateFormatsList: List[SimpleDateFormat] = List(
    new SimpleDateFormat("yyyy-MM-dd"),
    new SimpleDateFormat("dd-MM-yyyy"),
    new SimpleDateFormat("yyyy/MM/dd"),
    new SimpleDateFormat("dd/MM/yyyy"))

  //---------------------------------------------------------
  //# AUXILIARY
  //---------------------------------------------------------

  def getCurrentDate: Date = {
    Calendar.getInstance().getTime
  }

  /**
   * Auxiliary method that returns the current date in the Timestamp format.
   */
  def getCurrentDateTimestamp: Timestamp = {
    new Timestamp(getCurrentDate.getTime)
  }

  /**
   * Converts a String to a Date by providing the Date and a String specifying the date format.
   *
   * @param date String given to convert to Date.
   * @param format String specifying the date format.
   * @return String of the given date.
   */
  def stringToDateFormat(date: String, format: String): Date = {
    val sdf = new SimpleDateFormat(format)
    sdf.parse(date)
  }

  def stringToLocalDateFormat(date: String, format: String): LocalDate = {
    dateToLocalDate(stringToDateFormat(date, format))
  }

  def dayOfWeekToDayTypeString(dayOfWeek: Int): DayType = {
    if ((2 to 6).contains(dayOfWeek)) DayType.Weekday
    else DayType.Weekend
  }

  def dateToDayOfWeekInt(date: LocalDate): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(localDateToDate(date))
    calendar.get(Calendar.DAY_OF_WEEK)
  }

  def dateToDayTypeString(date: LocalDate): DayType = {
    val dayOfWeek = dateToDayOfWeekInt(date)
    if (dayOfWeek == 1 || dayOfWeek == 7) DayType.Weekend
    else DayType.Weekday
  }

  def removeTimeFromDate(date: Date): Date = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val string = sdf.format(date)
    sdf.parse(string)
  }

  def dateToLocalDate(date: Date): LocalDate = {
    date.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
  }

  def localDateToDate(localDate: LocalDate): Date = {
    localDate.isLeapYear
    java.sql.Date.valueOf(localDate)
  }

  def isLeapYear(year: Option[Int]): Boolean = {
    if (year.isDefined) (year.get % 4 == 0 || year.get % 400 == 0) && year.get % 100 != 0
    else false
  }

  /**
   * Converts a Date to a String by providing the Date and a String specifying the date format.
   * @param date Date given to convert to string.
   * @param format String specifying the date format.
   * @return String of the given date.
   */
  def dateToStringFormat(date: Date, format: String): String = {
    val sdf = new SimpleDateFormat(format)
    sdf.format(date)
  }

  def localDateToStringFormat(date: LocalDate, format: String): String = {
    dateToStringFormat(localDateToDate(date), format)
  }

  def getDateWithAddedSeconds(date: Date, seconds: Int): Date = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    cal.add(Calendar.SECOND, seconds)
    cal.getTime
  }

  def getDateWithSubtractedSeconds(date: Date, seconds: Int): Date = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    cal.setLenient(true)
    cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) - seconds)
    cal.getTime
  }

  def getDifferenceInDays(startDateMillis: Long, endDateMillis: Long): Int = {
    val time: Double = endDateMillis - startDateMillis
    Math.ceil(time / (1000 * 60 * 60 * 24)).toInt
  }

  /**
   * Tries do parse a String and convert it into a Date with a valid format. (see dateFormatsList above)
   * @param date String specifying the date to be parsed.
   * @return Some(Date) if the date could be parsed. None otherwise
   */
  def parseDate(date: String): Option[Date] = {
    dateFormatsList.flatMap { format =>
      format.setLenient(false)
      format.setTimeZone(TimeZone.getDefault)
      Try(Some(format.parse(date))).getOrElse(None)
    }.headOption
  }

  def parseLocalDate(date: String): Option[LocalDate] = {
    localDateFormatsList.flatMap { format =>
      format.setLenient(false)
      format.setTimeZone(TimeZone.getDefault)
      Try(Some(format.parse(date))).getOrElse(None)
    }.headOption.map(dateToLocalDate)
  }

  def parseTimezone(string: String): Option[TimeZone] = {
    val timezone = TimeZone.getTimeZone(string)
    if (!string.equals("GMT") && timezone.getID.equals("GMT")) None
    else Some(timezone)
  }

  def parseDateWithTimezone(date: String, timezone: String): Option[Date] = {
    dateFormatsList.flatMap { format =>
      format.setLenient(false)
      format.setTimeZone(parseTimezone(timezone).get)
      Try(Some(format.parse(date))).getOrElse(None)
    }.headOption
  }

  def isPossibleDate(day: Int, month: Int, year: Int): Boolean = {
    val dateCalendar = Calendar.getInstance
    dateCalendar.setLenient(false)
    dateCalendar.set(year, month - 1, day)
    val result = Try(Some(dateCalendar.getTime)).getOrElse(None)
    result.isDefined
  }

  /*def isPossibleDate(day: Int, month: Int, year: Int): Boolean = {
    val dateCalendar = Calendar.getInstance
    dateCalendar.setLenient(false)
    val result = Try(Some(dateCalendar.set(year, month, day))).getOrElse(None)
    (day == 29 && month == 2 && !isLeapYear(Some(year))) || result.isDefined
  }*/

  def isPossibleDateWithoutYear(day: Int, month: Int): Boolean = {
    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
      day match {
        case 29 => month != 2
        case 30 => month != 2
        case 31 => month != 2 && month != 4 && month != 6 && month != 9 && month != 11
        case _ => true
      }
    } else false
  }

  def isPossibleDateWithoutMonth(day: Int, year: Int): Boolean = {
    val dateCalendar = Calendar.getInstance
    dateCalendar.setLenient(false)
    def iter(month: Int): Boolean = {
      if (month >= 12) false
      else {
        dateCalendar.set(year, month - 1, day)
        val result = Try(Some(dateCalendar.getTime)).getOrElse(None)
        if (result.isDefined) true
        else iter(month + 1)
      }
    }
    iter(0)
  }

}
