package database.repositories

import api.dtos.TaskDTO
import database.mappings.TaskMappings.TaskRow
import scala.concurrent.Future


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
  def deleteAllTasks: Future[Int]

  /**
    * Given a na id deletes the corresponding task
    *
    * @param id - identifier of the task to be deleted
    */
  def deleteTaskById(id: Int): Future[Int]

  /**
    * Updates a single task given its identifier
    *
    * @param id   - identifier of the task to be updated
    * @param task - information to update the task with
    * @return an Int with information of the updated task
    */
  def updateTaskById(id: Int, task: TaskDTO): Future[Int]

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
