package database.repositories.slick

import java.util.UUID

import api.dtos.TaskDTO
import api.services.{PeriodType, SchedulingType}
import database.mappings.TaskMappings.{TaskRow, _}
import database.repositories.TaskRepository
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import database.repositories.FileRepository
import api.utils.DTOUtils._

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
          fileRepo.selectFileNameFromFileId(elem.fileId).map{ name =>
            elem.period match{
              case 0 /*RunOnce*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.RunOnce)
              case 1 /*Minutely*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Minutely), elem.value, elem.endDateAndTime, elem.currentOccurrences)
              case 2 /*Hourly*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Hourly), elem.value, elem.endDateAndTime, elem.currentOccurrences)
              case 3 /*Daily*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Daily), elem.value, elem.endDateAndTime, elem.currentOccurrences)
              case 4 /*Weekly*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Weekly), elem.value, elem.endDateAndTime, elem.currentOccurrences)
              case 5 /*Monthly*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Monthly), elem.value, elem.endDateAndTime, elem.currentOccurrences)
              case 6 /*Yearly*/=> TaskDTO(elem.taskId, elem.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Yearly), elem.value, elem.endDateAndTime, elem.currentOccurrences)
            }
          }
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
  def selectTaskByTaskId(id: String): Future[TaskDTO] = {
    exec(selectByTaskId(id).result).map{seq =>
      taskRowToTaskDTO(seq.head)
    }
  }


  def selectFileIdByTaskId(id: String): Future[String] = {
    selectTaskByTaskId(id).flatMap{
      elem => fileRepo.selectFileIdFromFileName(elem.fileName) //TODO: Improve implementation.
    }
  }

  def selectTotalOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    exec(selectByTaskId(id).result.head.map(_.totalOccurrences))
  }

  def selectCurrentOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    exec(selectByTaskId(id).result.head.map(_.currentOccurrences))
  }

  def decrementCurrentOccurrencesByTaskId(id: String): Unit = {
    selectCurrentOccurrencesByTaskId(id).map{
      elem => exec(selectByTaskId(id).map(_.currentOccurrences).update(Some(elem.get - 1)))
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
    fileRepo.existsCorrespondingFileName(task.fileName).flatMap {exists =>
      if(exists) exec(insertTask(taskDTOToTaskRow(task))).map(i => i == 1)
      else Future.successful(false)
    }
  }
}
