package api.controllers

import api.dtos.TaskDTO
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import api.services.TaskService._
import database.repositories.{FileRepository, TaskRepository}
import scala.concurrent.{ExecutionContext, Future}


/**
  * This controller handles the HTTP requests that are related to task scheduling.
  *
  * @param cc standard controller components
  */
@Singleton
class TaskController @Inject()(cc: ControllerComponents, fileRepo: FileRepository, taskRepo: TaskRepository)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  /**
    * Method that runs when a GET request is made on localhost:9000/
    *
    * @return HTTP Response with an OK, meaning all went well.
    */
  def index: Action[AnyContent] = Action {
    Ok("It works!")
  }

  /**
    * Method that runs when a POST request is made on localhost:9000/schedule and a JSON body is sent.
    * This method is used to handle a schedule task request on a particular file.
    *
    * @return HTTP Response with an OK, meaning all went well.
    *         HTTP Response with a BadRequest, meaning something went wrong and returns the errors.
    */
  def schedule: Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[TaskDTO]
    jsonResult.fold(
      errors =>
        Future.successful(BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))), //TODO - create object Error (extends DefaultHttpErrorHandler)
      task => {
        taskRepo.insertInTasksTable(TaskDTO(task.startDateAndTime, task.fileName))
        scheduleTask(task.fileName, task.startDateAndTime)
        Future.successful(Ok("Task inserted"))
      }
    )
  }

  /**
    * Method that gets all the scheduled tasks from the database
    *
    * @return HTTP Response OK with all the scheduled tasks
    */
  def getSchedule: Action[AnyContent] = Action.async {
    taskRepo.selectAllTasks.map { seq =>
      val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
      Ok(result)
    }
  }

  /**
    * Method that gets the task with the id given
    *
    * @param id - identifier of the task we are looking for
    * @return the task corresponding to the given id
    */
  def getScheduleById(id: Int): Action[AnyContent] = Action.async { //TODO - Error handling ID
    taskRepo.selectTaskById(id).map { seq =>
      val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
      Ok(result)
    }
  }

  /**
    * This method updates a Task given its ID and a JSON is passed with the information to be
    * altered
    *
    * @param id - identifier of the task to be modified
    * @return An HTTP response that is Ok if the task was updated or BadRequest if there was an error
    */
  def updateTask(id: Int): Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[TaskDTO]
    jsonResult.fold( //TODO - create new DTO, rename taskDTO to CreateTaskDTO
      errors => Future.successful(BadRequest("Error updating scheduled task: \n" + errors)),
      task =>  taskRepo.updateTaskById(id, task).map { i =>
        if (i > 0) Ok("Task with id = " + id + " was updated")
        else BadRequest("Task does not exist")
      }
    )
  }

  /**
    * This method deletes a task according to its ID
    *
    * @param id - identifier of the task to be deleted
    * @return An HTTP response that is Ok if the corresponding task was deleted or BadRequest if
    *         it wasn't.
    */
  def deleteTask(id: Int): Action[AnyContent] = Action.async {
    taskRepo.deleteTaskById(id).map { i =>
      if (i > 0) Ok("Task with id = " + id + " was deleted")
      else BadRequest("Error deleting file with given id")
    }
  }
}
