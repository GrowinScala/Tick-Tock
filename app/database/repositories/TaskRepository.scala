package database.repositories

import java.util.UUID

import akka.actor.FSM.Failure
import akka.actor.Status.Success
import api.dtos.{ TaskDTO, UpdateTaskDTO }
import database.mappings.TaskMappings.TaskRow

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Class that handles the data layer for the scheduled tasks.
 * It contains task scheduling related queries to communicate with the database.
 *
 * @param db Database class that contains the database information.
 */
trait TaskRepository {

  def selectAllTasks: Future[Seq[TaskDTO]]

  /**
   * Select a single task from the database by giving its id.
   *
   * @param id - the identifier of the task.
   * @return a TaskDTO of the selected task.
   */
  def selectTask(id: String): Future[Option[TaskDTO]]

  /**
   *
   * Select the fileId from a task by giving its taskId.
   *
   * @param id - the identifier of the task.
   * @return a String containing the fileId.
   */
  def selectFileIdByTaskId(id: String): Future[Option[String]]

  /**
   * Select the totalOccurrences from a task on the database by giving its id.
   *
   * @param id - the identifier of the task.
   * @return an Int representing the totalOccurrences of the task.
   */
  def selectTotalOccurrencesByTaskId(id: String): Future[Option[Int]]

  /**
   * Select the currentOccurrences from a task on the database by giving its id
   *
   * @param id - the identifier of the task we want to select.
   * @return an Int representing the currentOccurrences of the task.
   */
  def selectCurrentOccurrencesByTaskId(id: String): Future[Option[Int]]

  /**
   *
   * Reduces the currentOccurrences from a task on the database by 1 by giving its id.
   *
   * @param id - the identifier of the task we want to select.
   */
  def decrementCurrentOccurrencesByTaskId(id: String): Future[Unit]

  /**
   * Deletes all tasks from the tasks table on the database.
   */
  def deleteAllTasks: Future[Int]

  /**
   * Given a an id deletes the corresponding task
   *
   * @param id - identifier of the task to be deleted
   */
  def deleteTaskById(id: String): Future[Int]

  /**
   * Updates a single task given its identifier.
   *
   * @param id   - identifier of the task to be updated
   * @param task - information to update the task with
   * @return an Int with information of the updated task
   */
  def updateTaskById(id: String, task: TaskDTO): Future[Int]

  /*/**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Future[Unit]

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Future[Unit]*/

  /**
   * Inserts a task (row) on the tasks table on the database.
   *
   * @param task TaskDTO to be inserted.
   */
  def insertInTasksTable(task: TaskDTO): Future[Boolean]
}
