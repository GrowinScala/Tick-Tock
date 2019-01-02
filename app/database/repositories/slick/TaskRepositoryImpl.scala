package database.repositories.slick

import api.dtos.TaskDTO
import database.mappings.TaskMappings.{TaskRow, _}
import database.repositories.TaskRepository
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  *
  * @param db Database class that contains the database information.
  */
class TaskRepositoryImpl(dtbase: Database) extends TaskRepository {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val fileRepo = new FileRepositoryImpl(dtbase)

  def exec[T](action: DBIO[T]): Future[T] = dtbase.run(action)

  /**
    * Selects all tasks from the tasks table on the database.
    *
    * @return
    */
  def selectAllTasks: Future[Seq[TaskDTO]] = {
    exec(selectAllFromTasksTable.result).flatMap { seq =>
      Future.sequence {
        seq.map { elem =>
          fileRepo.selectFileNameFromFileId(elem.fileId).map(name => TaskDTO(elem.startDateAndTime, name))
        }
      }
    }
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
  def deleteAllTasks: Future[Int] = {
    exec(deleteAllFromTasksTable)
  }

  /**
    * Deletes a single task from the table on the database
    *
    * @param id - identifier of the task to be deleted
    */
  def deleteTaskById(id: Int): Future[Int] = {
    exec(deleteByTaskId(id))
  }

  def updateTaskById(id: Int, task: TaskDTO): Future[Int] = {
    fileRepo.selectFileIdFromName(task.fileName).flatMap { fileId =>
      exec(updateTaskByTaskId(id, new TaskRow(id, fileId, task.startDateAndTime)))
    }
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
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Future[Boolean] = {
    fileRepo.existsCorrespondingFileName(task.fileName).flatMap { exists =>
      if(exists)
        fileRepo.selectFileIdFromName(task.fileName).flatMap(id => exec(insertTask(TaskRow(0, id, task.startDateAndTime))).map(i => i == 1))
      else Future.successful(false)
    }
  }
}
