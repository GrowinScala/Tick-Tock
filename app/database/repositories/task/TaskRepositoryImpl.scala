package database.repositories.task

import java.util.Date

import api.dtos.TaskDTO
import api.services.{ PeriodType, SchedulingType }
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.mappings.ExclusionMappings._
import database.mappings.SchedulingMappings._
import database.repositories.exclusion.ExclusionRepository
import database.repositories.scheduling.SchedulingRepository
import javax.inject.Inject
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }

/**
 * Class that handles the data layer for the scheduled tasks.
 * It contains task scheduling related queries to communicate with the database.
 *
 * @param dtbase Database class that contains the database information.
 */
class TaskRepositoryImpl @Inject() (dtbase: Database, exclusionRepo: ExclusionRepository, schedulingRepo: SchedulingRepository) extends TaskRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def taskRowToTaskDTO(maybeTask: Option[TaskRow]): Future[Option[TaskDTO]] = {
    val task = maybeTask.getOrElse(TaskRow("", "", 0))
    val result = for {
      exclusions <- dtbase.run(getExclusionByTaskId(task.taskId).result)
      schedulings <- dtbase.run(getSchedulingByTaskId(task.taskId).result)
    } yield (exclusions.map(exclusionRepo.exclusionRowToExclusionDTO).toList, schedulings.map(schedulingRepo.schedulingRowToSchedulingDTO).toList)
    result.flatMap {
      case (exclusionList, schedulingList) =>
        val exclusionListOption = if (exclusionList.isEmpty) None else Some(exclusionList)
        val schedulingListOption = if (schedulingList.isEmpty) None else Some(schedulingList)
        dtbase.run(getFileByFileId(task.fileId).map(_.fileName).result.headOption).map { maybeName =>
          val name = maybeName.getOrElse("")
          Some(task.period match {
            case 0 /*RunOnce*/ => TaskDTO(task.taskId, name, SchedulingType.RunOnce, task.startDateAndTime)
            case 1 /*Minutely*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Minutely), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 2 /*Hourly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Hourly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 3 /*Daily*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Daily), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 4 /*Weekly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Weekly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 5 /*Monthly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Monthly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 6 /*Yearly*/ => TaskDTO(task.taskId, name, SchedulingType.Periodic, task.startDateAndTime, Some(PeriodType.Yearly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption)
            case 7 /*Personalized Minutely*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Minutely), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
            case 8 /*Personalized Hourly*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Hourly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
            case 9 /*Personalized Daily*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Daily), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
            case 10 /*Personalized Weekly*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Weekly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
            case 11 /*Personalized Monthly*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Monthly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
            case 12 /*Personalized Yearly*/ => TaskDTO(task.taskId, name, SchedulingType.Personalized, task.startDateAndTime, Some(PeriodType.Yearly), task.value, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, exclusionListOption, schedulingListOption)
          })
        }
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
            task.periodType.get match {
              case PeriodType.Minutely =>
                TaskRow(task.taskId, fileId, 7, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Hourly =>
                TaskRow(task.taskId, fileId, 8, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Daily =>
                TaskRow(task.taskId, fileId, 9, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Weekly =>
                TaskRow(task.taskId, fileId, 10, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Monthly =>
                TaskRow(task.taskId, fileId, 11, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
              case PeriodType.Yearly =>
                TaskRow(task.taskId, fileId, 12, task.period, task.startDateAndTime, task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone)
            }
        }
      }

  }

  /**
   * Selects all tasks from the tasks table on the database.
   *
   * @return
   */
  def selectAllTasks(offset: Option[Int] = None, limit: Option[Int] = None): Future[Seq[TaskDTO]] = {

    val tasks = if (offset.isDefined && limit.isDefined)
      selectAllFromTasksTable.drop(offset.get).take(limit.get)
    else selectAllFromTasksTable

    dtbase.run(tasks.result).flatMap { seq =>
      Future.sequence {
        seq.map(elem => taskRowToTaskDTO(Some(elem)))
      }.map(elem => elem.flatten)
    }
  }

  /**
   * Select a single task from the database given an its id
   *
   * @param id - the identifier of the task we want to select
   * @return the selected task according to the id given
   */
  def selectTask(id: String) = {
    val result = for {
      seq <- dtbase.run(getTaskByTaskId(id).result)
      exclusionSeq <- dtbase.run(getExclusionByTaskId(id).result)
      schedulingSeq <- dtbase.run(getSchedulingByTaskId(id).result)
    } yield (seq, exclusionSeq, schedulingSeq)

    result.flatMap {
      {
        case (seq, exclusionSeq, schedulingSeq) =>
          seq.map(taskRow => taskRowToTaskDTO(Some(taskRow))).headOption match {
            case Some(futureTask) =>
              futureTask.map {
                {
                  case Some(task) =>
                    Some(TaskDTO(
                      task.taskId,
                      task.fileName,
                      task.taskType,
                      task.startDateAndTime,
                      task.periodType,
                      task.period,
                      task.endDateAndTime,
                      task.totalOccurrences,
                      task.currentOccurrences,
                      task.timezone,
                      if (exclusionSeq.isEmpty) None else Some(exclusionSeq.map(exclusionRow => exclusionRepo.exclusionRowToExclusionDTO(exclusionRow)).toList),
                      if (schedulingSeq.isEmpty) None else Some(schedulingSeq.map(schedulingRow => schedulingRepo.schedulingRowToSchedulingDTO(schedulingRow)).toList)))
                  case None => None
                }
              }
            case None => Future(None)
          }
      }
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
      if (exists) {
        val taskCode = taskDTOToTaskRow(task).flatMap(elem => dtbase.run(insertTask(elem)).map(i => i == 1))
        task.exclusions match {
          case Some(exclusionList) =>
            val exclusionCodeList = exclusionList.map(dto => exclusionRepo.insertInExclusionsTable(dto))
            val codeList = List(taskCode) ::: exclusionCodeList
            task.schedulings match {
              case Some(schedulingList) =>
                val schedulingCodeList = schedulingList.map(dto => schedulingRepo.insertInSchedulingsTable(dto))
                val finalCodeList = codeList ::: schedulingCodeList
                Future.successful(finalCodeList.map(Await.result(_, Duration.Inf)).forall(_ == true))
              case None =>
                Future.successful(codeList.map(Await.result(_, Duration.Inf)).forall(_ == true))
            }
          case None =>
            task.schedulings match {
              case Some(schedulingList) =>
                val schedulingCodeList = schedulingList.map(dto => schedulingRepo.insertInSchedulingsTable(dto))
                val finalCodeList = List(taskCode) ::: schedulingCodeList
                Future.successful(finalCodeList.map(Await.result(_, Duration.Inf)).forall(_ == true))
              case None =>
                taskCode
            }
        }
      } else Future.successful(false)
    }
  }
}
