package database.repositories

import akka.actor.FSM.Failure
import akka.actor.Status.Success
import api.dtos.TaskDTO
import database.mappings.TaskMappings
import database.mappings.TaskMappings.TaskRow
import database.mappings.TaskMappings._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  *
  * @param db Database class that contains the database information.
  */
trait TaskRepository {

  def selectAllTasks: Future[Seq[TaskDTO]]

  /**
    * Select a single task from the database given an its id
    *
    * @param id - the identifier of the task we want to select
    * @return the selected task according to the id given
    */
  def selectTaskById(id: Int): Future[Seq[TaskRow]]

  /**
    * Deletes all tasks from the tasks table on the database.
    */
  def deleteAllTasks: Unit

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Unit

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Unit

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Future[Boolean]
}
