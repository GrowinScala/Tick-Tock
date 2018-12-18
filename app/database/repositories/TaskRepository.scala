package database.repositories

import akka.actor.FSM.Failure
import akka.actor.Status.Success
import api.dtos.TaskDTO
import database.mappings.TaskMappings
import database.mappings.TaskMappings.TaskRow
import slick.jdbc.MySQLProfile.api._
import database.mappings.TaskMappings._
import slick.dbio.DBIO

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

  def selectAllTasks: Seq[TaskDTO]

  /**
    * Select a single task from the database given an its id
    *
    * @param id - the identifier of the task we want to select
    * @return the selected task according to the id given
    */
  def selectTaskById(id: Int): Future[Seq[TaskRow]] = {
    exec(selectByTaskId(id).result)
  }

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
  def dropTasksTable: Unit = {
    exec(dropTasksTableAction)
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskRow to be inserted.
    */
  def insertInTasksTable(task: TaskRow)(implicit ec: ExecutionContext): Future[Boolean] = { //TODO - Refactor this TaskRow
    fileRepo.existsCorrespondingFileId(task.fileId).flatMap { exists =>
      if (exists) exec(insertTask(task)).map { i => i == 1 }
      else Future.successful(false)
    }
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO)(implicit ec: ExecutionContext): Future[Boolean] = {
    fileRepo.existsCorrespondingFileName(task.fileName).flatMap { exists =>
      if (exists)
        fileRepo.selectFileIdFromName(task.fileName).flatMap(id => exec(insertTask(TaskRow(0, id, task.startDateAndTime))).map(i => i == 1))
      else Future.successful(false)
    }
  }
}
