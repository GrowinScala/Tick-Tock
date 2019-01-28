package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import api.services.SchedulingType.SchedulingType
import executionengine.ExecutionJob
import java.time.Duration

import api.dtos.TaskDTO
import api.services.PeriodType.PeriodType
import database.repositories.{FileRepository, TaskRepository, TaskRepositoryImpl}
import database.utils.DatabaseUtils._
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

/**
  * Object that contains all methods for the task scheduling related to the service layer.
  */
@Singleton
class TaskService @Inject()(implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository) {

  implicit val ec = ExecutionContext.global

  /**
    * Schedules a task by giving the storageName to be executed once immediately.
    * @param fileName Name of the file on the storage folder.
    */
  def scheduleTask(task: TaskDTO): Unit ={
    fileRepo.selectFileIdFromFileName(task.fileName).map{ fileId =>
      task.taskType match{
        case SchedulingType.RunOnce =>
          new ExecutionJob(task.taskId, fileId, SchedulingType.RunOnce, task.startDateAndTime)
        case SchedulingType.Periodic =>
          task.periodType.get match {
            case PeriodType.Minutely =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMinutes(task.period.get)), task.endDateAndTime).start
            case PeriodType.Hourly =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofHours(task.period.get)), task.endDateAndTime).start
            case PeriodType.Daily =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get)), task.endDateAndTime).start
            case PeriodType.Weekly =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 7)), task.endDateAndTime).start
            case PeriodType.Monthly =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 30)), task.endDateAndTime).start //TODO: Improve Monthly and Yearly period accuracy
            case PeriodType.Yearly =>
              new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 365)), task.endDateAndTime).start
          }
      }
    }
  }
}
