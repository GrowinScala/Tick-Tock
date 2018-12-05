package api.dtos

import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

/**
  * Data transfer object for the scheduled tasks on the service side.
  * @param startDateAndTime Date and time of when the task is executed.
  * @param taskName Name of the file that is executed.
  */
case class TaskDTO(
                    startDateAndTime: Date,
                    taskName: String
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

  /**
    * Method that constructs the TaskDTO giving strings as dates and making the date format validation and conversion from string to date.
    * @param startDateAndTime Date and time of when the task is executed in a String format.
    * @param taskName Name of the file that is executed.
    * @return the taskDTO if the date received is valid. Throws an IllegalArgumentException if it's invalid.
    */
  def construct(startDateAndTime: String, taskName: String): TaskDTO = {
    val date = getValidDate(startDateAndTime)
    if(date.isDefined) new TaskDTO(date.get, taskName)
    else throw new IllegalArgumentException("Invalid date format.")
    //TODO: JsError/JsonValidationError
  }

  /**
    * Implicit that defines how a TaskDTO is read from the JSON request.
    * This implicit is used on the TaskController when Play's "validate" method is called.
    */
  implicit val taskReads: Reads[TaskDTO] = (
    (JsPath \ "startDateAndTime").read[String] and
      (JsPath \ "taskName").read[String]
    ) (TaskDTO.construct _)

  /**
    * Implicit that defines how a TaskDTO is written to a JSON format.
    */
  implicit val taskWrites = new Writes[TaskDTO] {
    def writes(st: TaskDTO): JsValue = {
      Json.obj(
        "startDateAndTime" -> st.startDateAndTime,
        "taskName" -> st.taskName,
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

}
