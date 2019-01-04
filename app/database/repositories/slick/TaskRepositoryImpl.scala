package database.repositories.slick

import java.util.UUID

import api.dtos.TaskDTO
import database.mappings.TaskMappings.{TaskRow, _}
import database.repositories.TaskRepository
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import database.repositories.FileRepository

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  *
  * @param db Database class that contains the database information.
  */
class TaskRepositoryImpl(dtbase: Database) extends TaskRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
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
          fileRepo.selectFileNameFromFileId(elem.fileId).map(name => TaskDTO(elem.taskId, elem.startDateAndTime, name))
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
  def selectTaskById(id: String): Future[Seq[TaskDTO]] = {
    exec(selectByTaskId(id).result).flatMap { seq =>
      Future.sequence {
        seq.map { elem =>
          fileRepo.selectFileNameFromFileId(elem.fileId).map(name => TaskDTO(elem.taskId, elem.startDateAndTime, name))}
      }
    }
  }

  def selectFileIdFromTaskId(taskId: String): Future[String] = {
    selectTaskById(taskId).flatMap{
      seq => fileRepo.selectFileIdFromName(seq.head.fileName) //TODO: Improve implementation.
    }
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
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Future[Boolean] = {
    fileRepo.existsCorrespondingFileName(task.fileName).flatMap { exists =>
      if(exists)
        fileRepo.selectFileIdFromName(task.fileName).flatMap(fileId => exec(insertTask(TaskRow(task.taskId, fileId, task.startDateAndTime))).map(i => i == 1))
      else Future.successful(false)
    }
  }
}
