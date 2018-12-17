package database.repositories

import api.dtos.TaskDTO
import database.mappings.TaskMappings
import database.mappings.TaskMappings.TaskRow
import slick.jdbc.MySQLProfile.api._
import database.mappings.TaskMappings._
import slick.dbio.DBIO

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  *
  * @param db Database class that contains the database information.
  */
class TaskRepository(dtbase: Database) extends BaseRepository {

  val fileRepo = new FileRepository(dtbase)

  /**
    * Selects all tasks from the tasks table on the database.
    *
    * @return
    */
  def selectAllTasks: Future[Seq[TaskRow]] = {
    exec(selectAllFromTasksTable.result)
  }

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
  def deleteAllTasks: Unit = {
    exec(deleteAllFromTasksTable)
  }

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Unit = {
    exec(createTasksTableAction)
  }

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
  def insertInTasksTable(task: TaskRow)(implicit ec: ExecutionContext): Unit = { //TODO - Refactor this TaskRow
    fileRepo.existsCorrespondingFileId(task.fileId).map { f =>
      if (f) exec(insertTask(task))
      else println("Could not insert Task with id " + task.fileId + " due to not finding a corresponding File.")
    }
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskDTO to be inserted.
    */
  def insertTasksTableAction(task: TaskDTO)(implicit ec: ExecutionContext): Unit = {
    fileRepo.existsCorrespondingFileName(task.fileName).map { exists =>
      if (exists) fileRepo.selectFileIdFromName(task.fileName).map(id => exec(insertTask(TaskRow(0, id, task.startDateAndTime))))
      else println("Could not insert Task with name " + task.fileName + "due to not finding a corresponding File.")
    }
  }
}
