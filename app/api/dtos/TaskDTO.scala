package api.dtos

import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class TaskDTO(
                    startDateAndTime: Date,
                    taskName: String
                  )

object TaskDTO {

  val dateFormatsList: Array[SimpleDateFormat] = Array(
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  )

  def construct(startDateAndTime: String, taskName: String): TaskDTO ={
    val index = isValidDate(startDateAndTime)
    if(index != -1) new TaskDTO(dateFormatsList(index).parse(startDateAndTime), taskName)
    else throw new IllegalArgumentException("Invalid date format.")
  }

  implicit val taskReads: Reads[TaskDTO] = (
    (JsPath \ "startDateAndTime").read[String] and
      (JsPath \ "taskName").read[String]
    )(TaskDTO.construct _)

  implicit val taskWrites = new Writes[TaskDTO] {
    def writes(st: TaskDTO): JsValue = {
      Json.obj(
        "startDateAndTime" -> st.startDateAndTime,
        "taskName" -> st.taskName,
      )
    }
  }

  def isValidDate(date: String): Int = {

    def iter(array: Array[SimpleDateFormat], index: Int): Int = {
      try{
        array.head.setLenient(false)
        val d = array.head.parse(date)
        val s = array.head.format(d)
        index
      }
      catch{
        case _: Throwable =>
          if(array.tail.isEmpty) -1
          else iter(array.tail, index + 1)
      }

    }
    iter(dateFormatsList, 0)
  }

}
