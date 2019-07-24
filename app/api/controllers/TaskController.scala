package api.controllers

import api.dtos.{ CreateTaskDTO, UpdateTaskDTO }
import api.services.TaskService
import api.utils.UUIDGenerator
import api.validators.Error._
import api.validators.TaskValidator
import database.repositories.file.FileRepository
import database.repositories.exclusion.ExclusionRepository
import database.repositories.scheduling.SchedulingRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionManager
import javax.inject.{ Inject, Singleton }
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * This controller handles the HTTP requests that are related to task scheduling.
 *
 * @param cc standard controller components
 */
@Singleton
class TaskController @Inject() (cc: ControllerComponents)(implicit exec: ExecutionContext, implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val exclusionRepo: ExclusionRepository, implicit val schedulingRepo: SchedulingRepository, implicit val UUIDGen: UUIDGenerator, implicit val executionManager: ExecutionManager) extends AbstractController(cc) {

  val taskValidator = new TaskValidator()

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
    val jsonResult = request.body.validate[CreateTaskDTO]
    jsonResult.fold(
      errors =>
        Future.successful(BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))),
      task => {
        taskValidator.scheduleValidator(task).flatMap {
          {
            case Left(errorList) =>
              Future.successful(BadRequest(JsArray(errorList.map(error => Json.toJsObject(error)).toIndexedSeq)))
            case Right(taskDto) =>
              taskRepo.insertInTasksTable(taskDto)
              if (taskDto.exclusions.isDefined) taskDto.exclusions.get.foreach(elem => exclusionRepo.insertInExclusionsTable(elem))
              if (taskDto.schedulings.isDefined) taskDto.schedulings.get.foreach(elem => schedulingRepo.insertInSchedulingsTable(elem))
              val taskService = new TaskService
              taskService.scheduleTask(taskDto)
              val url = routes.TaskController.getScheduleById(taskDto.taskId).absoluteURL(request.secure)(request).stripSuffix("/").trim
              Future.successful(Ok("Task received => " + url))
          }
        }
      })
  }

  /**
   * Method that gets all the scheduled tasks from the database
   *
   * @return HTTP Response OK with all the scheduled tasks
   */
  def getSchedule: Action[AnyContent] = Action.async {
    taskRepo.selectAllTasks.map { seq =>
      val result = JsArray(seq.map(tr => Json.toJson(tr)))
      Ok(result)
    }
  }

  /**
   * Method that gets the task with the id given
   *
   * @param id - identifier of the task we are looking for
   * @return the task corresponding to the given id
   */
  def getScheduleById(id: String): Action[AnyContent] = Action.async {
    taskRepo.selectTask(id).map {
      case Some(task) => Ok(Json.toJson(task))
      case None => BadRequest(Json.toJsObject(invalidEndpointId))
    }
  }

  /**
   * This method updates a Task given its ID and a JSON is passed with the information to be
   * altered.
   *
   * @param id - identifier of the task to be modified
   * @return An HTTP response that is Ok if the task was updated or BadRequest if there was an error
   */
  def updateTask(id: String): Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[UpdateTaskDTO]
    jsonResult.fold(
      errors => Future.successful(BadRequest("Error replacing scheduled task : \n" + errors)),
      task => {
        taskValidator.updateValidator(id, task).flatMap(
          {
            case Left(errorList) =>
              Future.successful(BadRequest(JsArray(errorList.map(error => Json.toJsObject(error)).toIndexedSeq)))
            case Right(taskDto) =>
              taskRepo.updateTaskById(id, taskDto)
              val taskService = new TaskService
              taskService.replaceTask(id, taskDto)
              val url = routes.TaskController.getScheduleById(id).absoluteURL(request.secure)(request).stripSuffix("/").trim
              Future.successful(Ok("Task received => " + url))
          })
      })

  } //TODO - implement exception when periodicity is implemented

  /**
   * This method deletes a task according to its ID
   *
   * @param id - identifier of the task to be deleted
   * @return An HTTP response that is Ok if the corresponding task was deleted or BadRequest if
   *         it wasn't.
   */
  def deleteTask(id: String): Action[AnyContent] = Action.async {
    taskRepo.selectTask(id).map {
      case Some(_) =>
        taskRepo.deleteTaskById(id)
        NoContent
      case None => BadRequest(Json.toJsObject(invalidEndpointId))
    }
  }

  def replaceTask(id: String): Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[CreateTaskDTO]
    jsonResult.fold(
      errors =>
        Future.successful(BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))),
      task => {
        taskValidator.scheduleValidator(task).flatMap {
          {
            case Left(errorList) =>
              Future.successful(BadRequest(JsArray(errorList.map(error => Json.toJsObject(error)).toIndexedSeq)))
            case Right(taskDto) =>
              taskRepo.updateTaskById(id, taskDto)
              val taskService = new TaskService
              taskService.replaceTask(id, taskDto)
              val url = routes.TaskController.getScheduleById(id).absoluteURL(request.secure)(request).stripSuffix("/").trim
              Future.successful(Ok("Task received => " + url))
          }
        }
      })

  }
}
