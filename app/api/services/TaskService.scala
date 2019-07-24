package api.services

import java.time.Duration
import java.util.{ Calendar, Date }

import akka.actor.{ ActorRef, ActorSystem, Props }
import api.dtos.{ ExclusionDTO, SchedulingDTO, TaskDTO }
import api.services.Criteria.Criteria
import api.utils.DateUtils.{ dateToDayTypeString, _ }
import database.repositories.task.TaskRepository
import database.repositories.file.FileRepository
import executionengine.{ ExecutionJob, ExecutionManager }
import executionengine.ExecutionJob.{ Cancel, Start }
import javax.inject.{ Inject, Singleton }

import scala.collection._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Await, ExecutionContext, ExecutionContextExecutor }

/**
 * Object that contains all methods for the task scheduling related to the service layer.
 */
@Singleton
class TaskService @Inject() (implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val executionManager: ExecutionManager) {

  val system = ActorSystem("ExecutionSystem")
  implicit val sd: ExecutionContextExecutor = system.dispatcher

  implicit val ec: ExecutionContext = ExecutionContext.global

  var actorMap: scala.collection.immutable.Map[String, ActorRef] = scala.collection.immutable.Map[String, ActorRef]()

  /**
   * Schedules a task by giving the storageName to be executed once immediately.
   */
  def scheduleTask(task: TaskDTO): Unit = {

    val fileId = Await.result(fileRepo.selectFileIdFromFileName(task.fileName), scala.concurrent.duration.Duration.Inf)

    task.taskType match {

      case SchedulingType.RunOnce =>
        val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.RunOnce, task.startDateAndTime, None, None, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
        actorMap += (task.taskId -> actorRef)
        actorRef ! Start

      case SchedulingType.Personalized =>
        val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Personalized, task.startDateAndTime, None, task.endDateAndTime, task.timezone, calculateExclusions(task), calculateSchedulings(task), fileRepo, taskRepo, executionManager))
        actorMap += (task.taskId -> actorRef)
        actorRef ! Start

      case SchedulingType.Periodic =>

        task.periodType.get match {

          case PeriodType.Minutely =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMillis(task.period.get * 60000)), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start

          case PeriodType.Hourly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMillis(task.period.get * (60000 * 60))), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start

          case PeriodType.Daily =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMillis(task.period.get * (60000 * 60 * 24))), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start

          case PeriodType.Weekly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMillis(task.period.get * (60000 * 60 * 24 * 7))), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start

          case PeriodType.Monthly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMillis(task.period.get * (60000 * 60 * 24 * 30))), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start

          case PeriodType.Yearly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * (60000 * 60 * 24 * 365))), task.endDateAndTime, task.timezone, calculateExclusions(task), Nil, fileRepo, taskRepo, executionManager))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Start
        }
    }
  }

  def replaceTask(id: String, task: TaskDTO): Unit = {
    if (actorMap.contains(id)) {
      actorMap(id) ! Cancel
      actorMap -= id
    }
    scheduleTask(task)
  }

  def calculateExclusions(task: TaskDTO): List[Date] = {
    if (task.exclusions.isDefined) {
      val startCalendar = Calendar.getInstance
      val endCalendar = Calendar.getInstance
      val iterCalendar = Calendar.getInstance
      if (task.startDateAndTime.isDefined) startCalendar.setTime(task.startDateAndTime.get) else startCalendar.setTime(new Date())
      if (task.taskType != SchedulingType.RunOnce) {
        if (task.totalOccurrences.isDefined) {
          endCalendar.setTime(task.startDateAndTime.get)
          task.periodType.get match {
            case PeriodType.Yearly => endCalendar.add(Calendar.YEAR, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Monthly => endCalendar.add(Calendar.MONTH, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Weekly => endCalendar.add(Calendar.WEEK_OF_YEAR, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Daily => endCalendar.add(Calendar.DAY_OF_MONTH, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Hourly => endCalendar.add(Calendar.HOUR_OF_DAY, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Minutely => endCalendar.add(Calendar.MINUTE, task.period.get * (task.currentOccurrences.get - 1))
          }
        } else endCalendar.setTime(task.endDateAndTime.get)
      }
      iterCalendar.setTime(startCalendar.getTime)
      iterCalendar.add(Calendar.DAY_OF_MONTH, -1)
      val dayDifference = getDifferenceInDays(startCalendar.getTimeInMillis, endCalendar.getTimeInMillis)
      var returnList: List[Date] = Nil
      task.exclusions.get.foreach { exclusion =>

        iterCalendar.setTime(startCalendar.getTime)
        iterCalendar.add(Calendar.DAY_OF_MONTH, -1)
        var list = ListBuffer[Date]()

        exclusion match {
          case ExclusionDTO(_, _, Some(date), None, None, None, None, None, None) => if (isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList
          case ExclusionDTO(_, _, None, Some(day), None, None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) equals dayType) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, None, Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case ExclusionDTO(_, _, None, Some(day), None, None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, None, Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year && criteria == Criteria.First) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case _ => println("Exclusion borked.")

        }
      }
      returnList.sortBy(_.getTime)
    } else Nil
  }

  def calculateSchedulings(task: TaskDTO): List[Date] = {
    if (task.schedulings.isDefined) {
      val startCalendar = Calendar.getInstance
      val endCalendar = Calendar.getInstance
      val iterCalendar = Calendar.getInstance
      if (task.startDateAndTime.isDefined) startCalendar.setTime(task.startDateAndTime.get) else startCalendar.setTime(new Date())
      if (task.taskType != SchedulingType.RunOnce) {
        if (task.totalOccurrences.isDefined) {
          endCalendar.setTime(task.startDateAndTime.get)
          task.periodType.get match {
            case PeriodType.Yearly => endCalendar.add(Calendar.YEAR, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Monthly => endCalendar.add(Calendar.MONTH, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Weekly => endCalendar.add(Calendar.WEEK_OF_YEAR, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Daily => endCalendar.add(Calendar.DAY_OF_MONTH, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Hourly => endCalendar.add(Calendar.HOUR_OF_DAY, task.period.get * (task.currentOccurrences.get - 1))
            case PeriodType.Minutely => endCalendar.add(Calendar.MINUTE, task.period.get * (task.currentOccurrences.get - 1))
          }
        } else endCalendar.setTime(task.endDateAndTime.get)
      }
      iterCalendar.setTime(startCalendar.getTime)
      iterCalendar.add(Calendar.DAY_OF_MONTH, -1)
      val dayDifference = getDifferenceInDays(startCalendar.getTimeInMillis, endCalendar.getTimeInMillis)
      var returnList: List[Date] = Nil
      task.schedulings.get.foreach { exclusion =>

        iterCalendar.setTime(startCalendar.getTime)
        iterCalendar.add(Calendar.DAY_OF_MONTH, -1)
        var list = ListBuffer[Date]()

        exclusion match {
          case SchedulingDTO(_, _, Some(date), None, None, None, None, None, None) => if (isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList
          case SchedulingDTO(_, _, None, Some(day), None, None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, None, Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), Some(year), None) =>
            returnList = getDateFromCalendar(day, month, year, task.timezone) :: returnList

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) returnList = iterCalendar.getTime :: returnList
            }

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime)) returnList = date :: returnList

          case SchedulingDTO(_, _, None, Some(day), None, None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, None, Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_MONTH) == day && iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
            for (_ <- 0 to dayDifference) {
              iterCalendar.add(Calendar.DAY_OF_MONTH, 1)
              if (iterCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek && dayOfWeekToDayTypeString(iterCalendar.get(Calendar.DAY_OF_WEEK)) == dayType && iterCalendar.get(Calendar.MONTH) == month && iterCalendar.get(Calendar.YEAR) == year) list += iterCalendar.getTime
            }
            returnList = getReturnListByCriteria(criteria, list, returnList)

          case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
            val date = getDateFromCalendar(day, month, year, task.timezone)
            if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date) && isDateBetweenLimits(date, startCalendar.getTime, endCalendar.getTime) && criteria == Criteria.First) returnList = date :: returnList

          case _ => println("Exclusion borked.")
        }
      }
      returnList.sortBy(_.getTime)
    } else Nil

  }

  //TODO change implementation
  private def getDateFromCalendar(day: Int, month: Int, year: Int, timezone: Option[String] = None): Date = {
    val dateCalendar = Calendar.getInstance
    if (timezone.isDefined) dateCalendar.setTimeZone(parseTimezone(timezone.get).get)
    dateCalendar.set(year, month, day)
    removeTimeFromDate(dateCalendar.getTime)
  }

  private def getDifferenceInDays(startDateMillis: Long, endDateMillis: Long): Int = {
    val time = endDateMillis - startDateMillis
    (time / (1000 * 60 * 60 * 24)).toInt
  }

  private def isDateBetweenLimits(date: Date, startDate: Date, endDate: Date): Boolean = {
    (date == startDate || date.after(startDate)) && (date == endDate || date.before(endDate))
  }

  private def getReturnListByCriteria(criteria: Criteria, dates: ListBuffer[Date], returnList: List[Date]): List[Date] = {
    criteria match {
      case first if first == Criteria.First && dates.nonEmpty => dates.head :: returnList
      case second if second == Criteria.Second && dates.size >= 2 => dates(1) :: returnList
      case third if third == Criteria.Third && dates.size >= 3 => dates(2) :: returnList
      case fourth if fourth == Criteria.Fourth && dates.size >= 4 => dates(3) :: returnList
      case last if last == Criteria.Last && dates.nonEmpty => dates.last :: returnList
      case _ => returnList
    }
  }
}
