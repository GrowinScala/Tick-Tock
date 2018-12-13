package controllers

import api.dtos.TaskDTO
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import api.services.TaskService._
import database.repositories.{FileRepository, TaskRepository}
import slick.jdbc.MySQLProfile.api._
import api.validators.Validator._

import scala.concurrent.Future


/**
  * This controller handles the HTTP requests that are related to task scheduling.
  * @param cc standard controller components
  */
class TaskController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepository(db)
  val taskRepo = new TaskRepository(db)

  /**
    * Method that runs when a GET request is made on localhost:9000/
    * @return HTTP Response with an OK, meaning all went well.
    */
  def index = Action {
    Ok("It works!")
  }

  /**
    * Method that runs when a POST request is made on localhost:9000/schedule and a JSON body is sent.
    * This method is used to handle a schedule task request on a particular file.
    * @return HTTP Response with an OK, meaning all went well.
    *         HTTP Response with a BadRequest, meaning something went wrong and returns the errors.
    */
  def schedule: Action[JsValue] = Action(parse.json).async { request =>
    taskParsingErrors(request.body) match {
      case Left(task) =>
        taskRepo.insertInTasksTable(TaskDTO(task.startDateAndTime, task.taskName))
        scheduleTask(fileRepo.selectStorageNameFromFileName(task.taskName), task.startDateAndTime)
        Future.successful(NoContent)
      case Right(jsArray) =>
        Future.successful(BadRequest(jsArray))
    }

    /*val jsonResult = request.body.validate[TaskDTO]
    jsonResult match {
      case s: JsSuccess[TaskDTO] =>
        val taskName = s.get.taskName
        val startDateAndTime = s.get.startDateAndTime
        taskRepo.insertInTasksTable(TaskDTO(startDateAndTime, taskName))
        scheduleTask(fileRepo.selectStorageNameFromFileName(taskName), startDateAndTime)
        Future.successful(NoContent)
      case e: JsError =>
        Future.successful(BadRequest("Errors: " + JsError.toJson(e).toString()))
    }*/


  }

}
