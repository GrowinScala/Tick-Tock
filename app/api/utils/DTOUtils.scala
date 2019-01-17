package api.utils

import api.dtos.TaskDTO
import api.services.{PeriodType, SchedulingType}
import database.mappings.TaskMappings.{TaskRow, insertTask}
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import database.utils.DatabaseUtils.DEFAULT_DB

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

object DTOUtils {

  implicit val ec = ExecutionContext.global
  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(DEFAULT_DB)

  def taskRowToTaskDTO(task: TaskRow): TaskDTO = {
    val name = Await.result(fileRepo.selectFileNameFromFileId(task.fileId), 5 seconds)
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

  def taskDTOToTaskRow(task: TaskDTO): TaskRow = {
    val fileId = Await.result(fileRepo.selectFileIdFromFileName(task.fileName), 5 seconds)
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
