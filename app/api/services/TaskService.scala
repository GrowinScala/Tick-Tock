package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import api.services.SchedulingType.SchedulingType
import executionengine.ExecutionJob
import java.time.Duration

import api.dtos.TaskDTO
import api.services.PeriodType.PeriodType
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import database.utils.DatabaseUtils._

import scala.concurrent.ExecutionContext

/**
  * Object that contains all methods for the task scheduling related to the service layer.
  */
object TaskService {

  implicit val ec = ExecutionContext.global
  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(DEFAULT_DB)

  /**
    * Schedules a task by giving the storageName to be executed once immediately.
    * @param fileName Name of the file on the storage folder.
    */
  def scheduleTask(task: TaskDTO): Unit ={
    fileRepo.selectFileIdFromFileName(task.fileName).map{ fileId =>
      task.taskType match{
        case SchedulingType.RunOnce =>
          new ExecutionJob(task.taskId, fileId, SchedulingType.RunOnce, task.startDateAndTime).start // run once
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

  /**
    * Schedules a task by giving the storageName to be executed once on the given datetime.
    * @param fileName Name of the file on the storage folder.
    * @param datetime Date specifying when the task is executed.
    */
  def scheduleOnce(taskId: String, startDate: Date): Unit = {
    fileRepo.selectFileIdFromName(taskId).map{ fileId =>
      new ExecutionJob(taskId, fileId ,SchedulingType.RunOnce, Some(startDate)).start
    }
  }

  /**
    * Schedules a task by giving the storageName to be executed either once or periodically, giving the datetime of when
    * it begins and the interval between
    */
  def scheduleWithPeriod(taskId: String, startDate: Date, periodType: PeriodType, interval: Long): Unit = {
    fileRepo.selectFileIdFromName(taskId).map { fileId =>
      periodType match {
        case PeriodType.Hourly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofHours(interval))).start
        case PeriodType.Daily =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval))).start
        case PeriodType.Weekly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 7))).start
        case PeriodType.Monthly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 30))).start //TODO: Improve Monthly and Yearly period accuracy
        case PeriodType.Yearly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 365))).start
      }
    }

  }

  def scheduleWithPeriod(taskId: String, startDate: Date, periodType: PeriodType, interval: Long, endDate: Date): Unit = {
    fileRepo.selectFileNameFromFileId(taskId).map { fileId =>
      periodType match {
        case PeriodType.Hourly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofHours(interval)), Some(endDate)).start
        case PeriodType.Daily =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval)), Some(endDate)).start
        case PeriodType.Weekly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 7)), Some(endDate)).start
        case PeriodType.Monthly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 30)), Some(endDate)).start
        case PeriodType.Yearly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 365)), Some(endDate)).start
      }
    }
  }

  def scheduleWithPeriod(taskId: String, startDate: Date, periodType: PeriodType, interval: Long, occurrences: Int): Unit = {
    fileRepo.selectFileNameFromFileId(taskId).map { fileId =>
      periodType match {
        case PeriodType.Hourly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofHours(interval)), None, Some(occurrences)).start
        case PeriodType.Daily =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval)), None, Some(occurrences)).start
        case PeriodType.Weekly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 7)), None, Some(occurrences)).start
        case PeriodType.Monthly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 30)), None, Some(occurrences)).start
        case PeriodType.Yearly =>
          new ExecutionJob(taskId, fileId, SchedulingType.Periodic, Some(startDate), Some(Duration.ofDays(interval * 365)), None, Some(occurrences)).start
      }
    }
  }
}
