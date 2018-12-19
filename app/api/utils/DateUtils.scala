package api.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import api.dtos.TaskDTO.fileRepo
import api.validators.Error
import api.validators.Error.{fileNameNotFound, invalidDateValue}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

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
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  )

  //---------------------------------------------------------
  //# VALIDATORS
  //---------------------------------------------------------

  /**
    * Method that validates a String representing a date and verifies if it follows any
    * of the permitted formats in the dateFormatsList and attempts to parse it accordingly.
    * @param date Date in a string format.
    * @return Returns the parsed date if the received String is valid encapsulated as an Option[Date].
    *         Returns None if not.
    */
  def getValidDate(date: String): Option[Date] = {
    dateFormatsList.flatMap { format =>
      format.setLenient(false)
      Try(Some(format.parse(date))).getOrElse(None)
    }.headOption
  }

  /**
    * Checks if the date given is valid, (if it already happened or not)
    * @param date The Date to be checked
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  def isValidDateValue(date: Date): Option[Error] = {
    val now = new Date()
    val currentDate = now.getTime
    val givenDate = date.getTime
    if(givenDate - currentDate > 0) None
    else Some(invalidDateValue)
  }

  /**
    * Checks if the given fileName exists.
    * @param fileName The fileName to be checked.
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  def isValidFileName(fileName: String): Future[Option[Error]] = {
    fileRepo.existsCorrespondingFileName(fileName).map(elem =>
      if(elem) None
      else Some(fileNameNotFound)
    )
  }

  //---------------------------------------------------------
  //# AUXILIARY
  //---------------------------------------------------------

  /**
    * Auxiliary method that returns the current date in the Timestamp format.
    */
  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime
    new Timestamp(now.getTime)
  }

  def stringToDateFormat(date: String, format: String): Date = {
    val sdf = new SimpleDateFormat(format)
    sdf.parse(date)
  }

  /**
    * Converts a Date to a String by providing the Date and a String specifying the date format.
    * @param date Date given to convert to string.
    * @param format String specifying the date format.
    * @return String of the given date.
    */
  def dateToStringFormat(date: Date, format: String): String ={
    val sdf = new SimpleDateFormat(format)
    sdf.format(date)
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
