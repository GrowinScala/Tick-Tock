package database.repositories.task

import java.util.Date

import api.dtos.TaskDTO
import api.services.{ PeriodType, SchedulingType }
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import javax.inject.Inject
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Class that handles the data layer for the scheduled tasks.
 * It contains task scheduling related queries to communicate with the database.
 *
 * @param dtbase Database class that contains the database information.
 */
class TaskRepositoryImpl @Inject() (dtbase: Database) extends TaskRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def taskRowToTaskDTO(maybeTask: Option[TaskRow]): Future[Option[TaskDTO]] = {
    val task = maybeTask.getOrElse(TaskRow("", "", 0))
    dtbase.run(getFileByFileId(task.fileId).map(_.fileName).result.headOption).map { maybeName =>
      val name = maybeName.getOrElse("")
      Some(task.period match {
        case 0 /*RunOnce*/ => TaskDTO(task.taskId, name, SchedulingType.RunOnce, task.startDateAndTime)
        case 1 /*Minutely*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Minutely), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
        case 2 /*Hourly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Hourly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
        case 3 /*Daily*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Daily), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
        case 4 /*Weekly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Weekly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
        case 5 /*Monthly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Monthly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
        case 6 /*Yearly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Yearly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
      })
    }
  }

  def taskDTOToTaskRow(task: TaskDTO): Future[TaskRow] = {
    dtbase.run(getFileByFileName(task.fileName).result.headOption)
      .map(maybeRow => maybeRow.getOrElse(FileRow("", "", new Date())).fileId)
      .map { fileId =>
        task.taskType match {
          case SchedulingType.RunOnce =>
            TaskRow(task.taskId, fileId, 0, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
          case SchedulingType.Periodic =>
            task.periodType.get match {
              case PeriodType.Minutely =>
                TaskRow(task.taskId, fileId, 1, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Hourly =>
                TaskRow(task.taskId, fileId, 2, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Daily =>
                TaskRow(task.taskId, fileId, 3, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Weekly =>
                TaskRow(task.taskId, fileId, 4, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Monthly =>
                TaskRow(task.taskId, fileId, 5, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Yearly =>
                TaskRow(task.taskId, fileId, 6, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
            }
          case SchedulingType.Personalized =>
            TaskRow(task.taskId, fileId, 7, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
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
          dtbase.run(getFileByFileId(elem.fileId).map(_.fileName).result.headOption).map { maybeName =>
            val name = maybeName.getOrElse("")
            elem.period match {
              case 0 /*RunOnce*/ => TaskDTO(elem.taskId, name, SchedulingType.RunOnce, elem.startDateAndTime)
              case 1 /*Minutely*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Minutely), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
              case 2 /*Hourly*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Hourly), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
              case 3 /*Daily*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Daily), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
              case 4 /*Weekly*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Weekly), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
              case 5 /*Monthly*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Monthly), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
              case 6 /*Yearly*/ => TaskDTO(elem.taskId, name, SchedulingType.Periodic, elem.startDateAndTime, Some(PeriodType.Yearly), elem.value, elem.endDateAndTime, elem.totalOccurrences, elem.currentOccurrences, elem.timezone)
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
  def selectTask(id: String): Future[Option[TaskDTO]] = {
    dtbase.run(getTaskByTaskId(id).result).flatMap { seq =>
      if (seq.isEmpty) Future.successful(None)
      else taskRowToTaskDTO(seq.headOption)
    }
  }

  def selectFileIdByTaskId(id: String): Future[Option[String]] = {
    selectTask(id).flatMap { elem =>
      if (elem.isDefined) dtbase.run(getFileByFileName(elem.get.fileName).result.headOption
        .map(maybeRow => maybeRow.getOrElse(FileRow("", "", new Date()))))
        .map(_.fileId).map(item => Some(item))
      else Future.successful(None)
    }
  }

  def selectTotalOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    dtbase.run(getTaskByTaskId(id).result.headOption)
      .map(maybeRow => maybeRow.getOrElse(TaskRow("", "", 0)))
      .map(_.totalOccurrences)
  }

  def selectCurrentOccurrencesByTaskId(id: String): Future[Option[Int]] = {
    dtbase.run(getTaskByTaskId(id).result.headOption
      .map(maybeRow => maybeRow.getOrElse(TaskRow("", "", 0)))
      .map(_.currentOccurrences))
  }

  def decrementCurrentOccurrencesByTaskId(id: String): Future[Int] = {
    selectCurrentOccurrencesByTaskId(id).flatMap {
      elem => dtbase.run(getTaskByTaskId(id).map(_.currentOccurrences).update(Some(elem.get - 1)))
    }
  }

  /**
   * Deletes a single task from the table on the database
   *
   * @param id - identifier of the task to be deleted
   */
  def deleteTaskById(id: String): Future[Int] = {
    dtbase.run(deleteTaskByTaskId(id))
  }

  /**
   * Updates a single task given its identifier
   *
   * @param id   - identifier of the task to be updated
   * @param task - information to update the task with
   * @return an Int with information of the updated task
   */
  def updateTaskById(id: String, task: TaskDTO): Future[Boolean] = {
    deleteTaskById(id)
    insertInTasksTable(task)
  }

  /**
   * Deletes all tasks from the tasks table on the database.
   */
  def deleteAllTasks: Future[Int] = {
    dtbase.run(deleteAllFromTasksTable)
  }

  /**
   * Inserts a task (row) on the tasks table on the database.
   *
   * @param task TaskDTO to be inserted.
   */
  def insertInTasksTable(task: TaskDTO): Future[Boolean] = {
    dtbase.run(getFileByFileName(task.fileName).exists.result).flatMap { exists =>
      if (exists) taskDTOToTaskRow(task).flatMap(elem => dtbase.run(insertTask(elem)).map(i => i == 1))
      else Future.successful(false)
    }
  }
}
