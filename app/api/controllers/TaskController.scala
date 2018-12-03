package controllers

import java.util.Date

import api.dtos.TaskDTO
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import api.services.TickTock
import play.api.libs.functional.syntax._
import api.validators.Validator._
import api.services.TaskService._
import database.mappings.TaskMappings._
import database.repositories.TaskRepository._
import database.repositories.FileRepository._

class TaskController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  /*object ScheduledTask {

    implicit val scheduledTaskReads: Reads[ScheduledTask] = (
      (JsPath \ "startDateAndTime").read[Date] and
        (JsPath \ "task").read[String]
        )(ScheduledTask.apply _)

    implicit val scheduledTaskWrites  = new Writes[ScheduledTask] {
      def writes(st: ScheduledTask): JsValue = {
        Json.obj(
          "startDateAndTime" -> st.startDateAndTime,
          "task" -> st.task
        )
      }
      /*(JsPath \ "startDateAndTime").write[Date] and
        (JsPath \ "task").write[String]
      )(unlift(ScheduledTask.unapply))*/
    }
  }

  case class ScheduledTask(startDateAndTime: Date, task: String)*/

  def index = Action {
    Ok("It works!")
  }

  def schedule: Action[JsValue] = Action(parse.json) { request =>
    val json = request.body
    val task = (json \ "task").as[String]
    val startDateAndTime = (json \ "startDateAndTime").as[String]
    val index = isValidDate(startDateAndTime)
    if(index != -1) {
      val date = dateFormatsList(index).parse(startDateAndTime)
      //if (isValidFilePath(task)) {
        insertTasksTableAction(TaskRow(0, selectFileIdFromName(task), date))
        scheduleOnce(task, date)
        Ok
      //}
      //else BadRequest("File path is incorrect.")
    }
    else BadRequest("Date format is incorrect.")
  }



    /*val jsonResult = request.body.validate[ScheduledTask]
    jsonResult match {
      case s: JsSuccess[ScheduledTask] =>
        val task = s.get.task
        val startDateAndTime = s.get.startDateAndTime
        println(startDateAndTime)
        DBConnector.insertTasksTableAction(Task(0, DBConnector.selectFileIdFromName(s.get.task).head, s.get.startDateAndTime))
        TickTock.scheduleOnce(task, startDateAndTime)
        Ok("JSON accepted.")
      case e: JsError =>
        BadRequest("JSON request doesn't meet the requirements.")
    }
  }*/

}
