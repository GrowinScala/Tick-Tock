package api.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.{ Calendar, Date, TimeZone }

import api.services.DayType
import api.services.DayType.DayType
import api.validators.Error
import database.repositories.{ FileRepository, FileRepositoryImpl }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import scala.concurrent.duration._
import database.utils.DatabaseUtils._
import javax.inject.{ Inject, Singleton }

object DateUtils {

  implicit val ec = ExecutionContext.global

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

  def dateToDayOfWeekInt(date: Date): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    calendar.get(Calendar.DAY_OF_WEEK)
  }

  def dateToDayTypeString(date: Date): DayType = {
    val dayOfWeek = dateToDayOfWeekInt(date)
    if (dayOfWeek >= 1 && dayOfWeek <= 5) DayType.Weekday
    else DayType.Weekend
  }

  def getTimeFromDate(date: Date): Date = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val string = sdf.format(date)
    sdf.parse(string)
  }

  def isLeapYear(year: Int): Boolean = year % 4 == 0

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
