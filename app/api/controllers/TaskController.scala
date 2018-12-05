package controllers

import api.dtos.TaskDTO
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import api.services.TaskService._
import database.repositories.TaskRepository._
import database.repositories.FileRepository._

class TaskController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  def index = Action {
    Ok("It works!")
  }

  def schedule: Action[JsValue] = Action(parse.json) { request =>

    val jsonResult = request.body.validate[TaskDTO]
    jsonResult match {
      case s: JsSuccess[TaskDTO] =>
        val taskName = s.get.taskName
        val startDateAndTime = s.get.startDateAndTime
        insertTasksTableAction(TaskDTO(startDateAndTime, taskName))
        scheduleOnce(selectFilePathFromFileName(taskName), startDateAndTime)
        Ok("JSON accepted.")
      case e: JsError =>
        BadRequest("Errors: " + JsError.toJson(e).toString())
    }
  }

}
