package api.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{ Calendar, Date, TimeZone }

import api.services.DayType
import api.services.DayType.DayType

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

  def getDayOfWeekFromDate(date: Date): Int = { // 1- Sun, 2- Mon, 3- Tue, 4- Wed, 5- Thu, 6- Fri, 7- Sat
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar.get(Calendar.DAY_OF_WEEK)
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

  def dayOfWeekToDayTypeString(dayOfWeek: Int): DayType = {
    if ((2 to 6).contains(dayOfWeek)) DayType.Weekday
    else DayType.Weekend
  }

  def dateToDayOfWeekInt(date: Date): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    calendar.get(Calendar.DAY_OF_WEEK)
  }

  def dateToDayTypeString(date: Date): DayType = {
    val dayOfWeek = dateToDayOfWeekInt(date)
    if (dayOfWeek == 1 || dayOfWeek == 7) DayType.Weekend
    else DayType.Weekday
  }

  def getTimeFromDate(date: Date): Date = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val string = sdf.format(date)
    sdf.parse(string)
  }

  def isLeapYear(year: Option[Int]): Boolean = {
    if (year.isDefined) year.get % 4 == 0
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

  def removeTimeFromDate(date: Date): Date = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val string = sdf.format(date)
    sdf.parse(string)
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
    val result = Try(Some(dateCalendar.set(day, month, year))).getOrElse(None)
    result.isDefined
  }

  def isPossibleDateWithoutYear(day: Int, month: Int): Boolean = {
    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
      val dateCalendar = Calendar.getInstance
      dateCalendar.setLenient(false)
      day match {
        case 29 => month != 2
        case 30 => month != 2
        case 31 => month != 2 || month != 4 || month != 6 || month != 9 || month != 11
        case _ => true
      }
    } else false
  }

  def isPossibleDateWithoutMonth(day: Int, year: Int): Boolean = {
    val dateCalendar = Calendar.getInstance
    dateCalendar.setLenient(false)
    def iter(month: Int): Boolean = {
      if (month >= 13) false
      else {
        val result = Try(Some(dateCalendar.set(day, month, year))).getOrElse(None)
        if (result.isDefined) true
        else iter(month + 1)
      }
    }
    iter(1)
  }

  /**
   * Method that returns the current time in a HH:mm:ss.SSS string format.
   * @return String of the current date.
   */
  def getSpecificCurrentTime: String = {
    val now = new Date()
    val sdf = new SimpleDateFormat("HH:mm:ss.SSS")
    sdf.format(now)
  }

}
