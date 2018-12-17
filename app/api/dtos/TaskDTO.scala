package api.dtos

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.japi
import api.validators.Error
import database.repositories.FileRepository
import slick.jdbc.MySQLProfile.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import api.validators.Error._

import scala.util.Try

/**
  * Data transfer object for the scheduled tasks on the service side.
  * @param startDateAndTime Date and time of when the task is executed.
  * @param taskName Name of the file that is executed.
  */
case class TaskDTO(
                    startDateAndTime: Date,
                    fileName: String
                  )

/**
  * Companion object for the TaskDTO
  */
object TaskDTO {

  /**
    * List of permitted date formats for the startDateAndTime field.
    * It is used for validation for received date strings through the HTTP request.
    */
  val dateFormatsList: List[SimpleDateFormat] = List(
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  )

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepository(db)

  /**
    * Method that constructs the TaskDTO giving strings as dates and making the date format validation and conversion from string to date.
    * @param startDateAndTime Date and time of when the task is executed in a String format.
    * @param fileName Name of the file that is executed.
    * @return the taskDTO if the date received is valid. Throws an IllegalArgumentException if it's invalid.
    */
  def construct(startDateAndTime: String, fileName: String): TaskDTO = {
    val date = getValidDate(startDateAndTime)
    TaskDTO(date.get, fileName)
  }

  /**
    * Implicit that defines how a TaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val taskReads: Reads[TaskDTO] = (
    (JsPath \ "startDateAndTime").read[String] and
      (JsPath \ "fileName").read[String]
    ) (TaskDTO.construct _)

  /**
    * Implicit that defines how a TaskDTO is written to a JSON format.
    */
  implicit val taskWrites = new Writes[TaskDTO] {
    def writes(st: TaskDTO): JsValue = {
      Json.obj(
        "startDateAndTime" -> st.startDateAndTime,
        "fileName" -> st.fileName,
      )
    }
  }

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
  def isValidFileName(fileName: String): Option[Error] = {
    if(fileRepo.existsCorrespondingFileName(fileName)) None
    else Some(fileNameNotFound)
  }





}
