package database.repositories

import api.dtos.TaskDTO
import api.services.{PeriodType, SchedulingType}
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import javax.inject.Inject
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  *
  * @param db Database class that contains the database information.
  */
class TaskRepositoryImpl(dtbase: Database) extends TaskRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def taskRowToTaskDTO(task: TaskRow): Future[TaskDTO] = {
    dtbase.run(selectById(task.fileId).map(_.fileName).result.head).map{ name =>
      task.period match{
        case 0 /*RunOnce*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.RunOnce)
        case 1 /*Minutely*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Minutely), task.value, task.endDateAndTime, task.currentOccurrences)
        case 2 /*Hourly*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Hourly), task.value, task.endDateAndTime, task.currentOccurrences)
        case 3 /*Daily*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Daily), task.value, task.endDateAndTime, task.currentOccurrences)
        case 4 /*Weekly*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Weekly), task.value, task.endDateAndTime, task.currentOccurrences)
        case 5 /*Monthly*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Monthly), task.value, task.endDateAndTime, task.currentOccurrences)
        case 6 /*Yearly*/=> TaskDTO(task.taskId, task.startDateAndTime, name, SchedulingType.Periodic, Some(PeriodType.Yearly), task.value, task.endDateAndTime, task.currentOccurrences)
      }
    }
  }

  private def taskDTOToTaskRow(task: TaskDTO): Future[TaskRow] = {
    dtbase.run(selectByFileName(task.fileName).result.head.map(_.fileId)).map { fileId =>
      task.taskType match {
        case SchedulingType.RunOnce =>
          TaskRow(task.taskId, fileId, 0, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
        case SchedulingType.Periodic =>
          task.periodType.get match {
            case PeriodType.Minutely =>
              TaskRow(task.taskId, fileId, 1, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
            case PeriodType.Hourly =>
              TaskRow(task.taskId, fileId, 2, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
            case PeriodType.Daily =>
              TaskRow(task.taskId, fileId, 3, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
            case PeriodType.Weekly =>
              TaskRow(task.taskId, fileId, 4, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
            case PeriodType.Monthly =>
              TaskRow(task.taskId, fileId, 5, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
            case PeriodType.Yearly =>
              TaskRow(task.taskId, fileId, 6, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences)
          }
      }

    }
  }

  /**
    * Selects all tasks from the tasks table on the database.
    *
    * @return
    */
  def selectAllTasks: Future[Seq[TaskDTO]] = {
    dtbase.run(selectAllFromTasksTable.result).flatMap { seq =>
      Future.sequence {
        seq.map { elem =>
          dtbase.run(selectById(elem.fileId).map(_.fileName).result.head).map{ name =>
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
    dtbase.run(selectByTaskId(id).result).flatMap(seq => taskRowToTaskDTO(seq.head))
  }

  def selectFileIdByTaskId(id: String): Future[String] = {
    selectTaskByTaskId(id).flatMap{
      elem => dtbase.run(selectByFileName(elem.fileName).result.head.map(_.fileId)) //TODO: Improve implementation.
    }
  }

  def selectTotalOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    dtbase.run(selectByTaskId(id).result.head.map(_.totalOccurrences))
  }

  def selectCurrentOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    dtbase.run(selectByTaskId(id).result.head.map(_.currentOccurrences))
  }

  def decrementCurrentOccurrencesByTaskId(id: String): Future[Unit] = {
    selectCurrentOccurrencesByTaskId(id).map{
      elem => Await.result(dtbase.run(selectByTaskId(id).map(_.currentOccurrences).update(Some(elem.get - 1))), 5 seconds)
    }
  }

  /**
    * Deletes all tasks from the tasks table on the database.
    */
  def deleteAllTasks: Future[Int] = {
    dtbase.run(deleteAllFromTasksTable)
  }

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Future[Unit] = {
    dtbase.run(createTasksTableAction)
  }

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Future[Unit] = {
    dtbase.run(dropTasksTableAction)
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    *
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Future[Boolean] = {
    dtbase.run(selectByFileName(task.fileName).exists.result).flatMap {exists =>
      if(exists) taskDTOToTaskRow(task).flatMap(elem => dtbase.run(insertTask(elem)).map(i => i == 1))
      else Future.successful(false)
    }
  }
}