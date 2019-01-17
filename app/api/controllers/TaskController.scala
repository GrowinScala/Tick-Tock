package api.controllers

import java.util.UUID
import api.dtos.{TaskDTO, CreateTaskDTO}
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import api.services.TaskService._
import database.repositories.{FileRepository, TaskRepository}
import api.validators.Validator._

import scala.concurrent.{ExecutionContext, Future}


/**
  * This controller handles the HTTP requests that are related to task scheduling.
  *
  *
  * @param cc standard controller components
  */
@Singleton
class TaskController @Inject()(cc: ControllerComponents, fileRepo: FileRepository, taskRepo: TaskRepository)(implicit exec: ExecutionContext) extends AbstractController(cc){

  /**
    * Method that runs when a GET request is made on localhost:9000/
    *
    * @return HTTP Response with an OK, meaning all went well.
    */
  def index: Action[AnyContent] = Action {
    Ok("It works!")
  }

  /**
    * Method that runs when a POST request is made on localhost:9000/task and a JSON body is sent.
    * This method is used to handle a schedule task request on a particular file.
    *
    * @return HTTP Response with an OK, meaning all went well.
    *         HTTP Response with a BadRequest, meaning something went wrong and returns the errors.
    */
  def schedule: Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[CreateTaskDTO]
    jsonResult.fold(
      errors =>
        Future.successful(BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))), //TODO - create object Error (extends DefaultHttpErrorHandler)
      task => {
        val validationResult = scheduleValidator(task)
        if(validationResult.isDefined)
          Future.successful(BadRequest(JsArray(validationResult.get.map(error => Json.toJsObject(error)).toIndexedSeq)))
        else{
          println(task.startDateAndTime)
          val taskDto: TaskDTO = TaskDTO(UUID.randomUUID().toString, task.startDateAndTime, task.fileName, task.taskType, task.periodType, task.period, task.endDateAndTime, task.occurrences, task.occurrences)
          taskRepo.insertInTasksTable(taskDto)
          scheduleTask(taskDto)
          Future.successful(Ok("Task received."))
        }
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
    * @return the task corresponding to the given idl
    */
  def getScheduleById(id: String): Action[AnyContent] = Action.async { //TODO - Error handling ID
    taskRepo.selectTaskByTaskId(id).map { elem =>
      //val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
      Ok(Json.toJsObject(elem))
    }
  }

  /**
    * Method that runs when a PATCH request is made on localhost:9000/task and a JSON body is sent.
    * This method is used to handle updates on a particular scheduled task by giving its id.
    *
    * @param id taskId of the task to be updated.
    * @return HTTP Response with an OK, meaning all went well.
    *         HTTP Response with a BadRequest, meaning something went wrong and returns the errors.
    */
  def updateTask(id: String): Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val jsonResult = request.body.validate[TaskDTO]
    jsonResult.fold( //TODO - create new DTO, rename taskDTO to CreateTaskDTO
      errors => Future.successful(BadRequest("Error updating scheduled task: \n" + errors)),
      task => Future.successful(Ok("Something"))
    )
  }
}
