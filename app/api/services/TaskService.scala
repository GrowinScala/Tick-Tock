package api.services

import java.time.Duration
import java.util.{Calendar, Date}

import akka.actor.{ActorRef, ActorSystem, Props}
import api.dtos.{ExclusionDTO, SchedulingDTO, TaskDTO}
import api.utils.DateUtils.{dateToDayTypeString, _}
import database.repositories.{FileRepository, TaskRepository}
import executionengine.ExecutionJob
import executionengine.ExecutionJob.{Cancel, Execute}
import javax.inject.{Inject, Singleton}

import scala.collection._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

/**
 * Object that contains all methods for the task scheduling related to the service layer.
 */
@Singleton
class TaskService @Inject() (implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository) {

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
        val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.RunOnce, task.startDateAndTime, None, None, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
        actorMap += (task.taskId -> actorRef)
        actorRef ! Execute

      case SchedulingType.Personalized =>
        val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Personalized, task.startDateAndTime, None, task.endDateAndTime, task.timezone, calculateExclusions(task), calculateSchedulings(task), fileRepo, taskRepo))
        actorMap += (task.taskId -> actorRef)
        actorRef ! Execute

      case SchedulingType.Periodic =>

        task.periodType.get match {

          case PeriodType.Minutely =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMinutes(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute

          case PeriodType.Hourly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofHours(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute

          case PeriodType.Daily =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute

          case PeriodType.Weekly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 7)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute

          case PeriodType.Monthly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 30)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute

          case PeriodType.Yearly =>
            val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 365)), task.endDateAndTime, task.timezone, calculateExclusions(task), None, fileRepo, taskRepo))
            actorMap += (task.taskId -> actorRef)
            actorRef ! Execute
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

  def calculateExclusions(task: TaskDTO): Option[mutable.Queue[Date]] = {
    if (task.exclusions.isDefined) {
      val startCalendar = Calendar.getInstance
      val endCalendar = Calendar.getInstance
      if (task.startDateAndTime.isDefined) startCalendar.setTime(task.startDateAndTime.get) else startCalendar.setTime(new Date())
      if (task.taskType != SchedulingType.RunOnce) {
        if (task.totalOccurrences.isDefined) {
          endCalendar.setTime(task.startDateAndTime.get)
          task.periodType.get match {
            case PeriodType.Yearly => endCalendar.add(Calendar.YEAR, task.currentOccurrences.get)
            case PeriodType.Monthly => endCalendar.add(Calendar.MONTH, task.currentOccurrences.get)
            case PeriodType.Weekly => endCalendar.add(Calendar.WEEK_OF_YEAR, task.currentOccurrences.get)
            case PeriodType.Daily => endCalendar.add(Calendar.DAY_OF_MONTH, task.currentOccurrences.get)
            case PeriodType.Hourly => endCalendar.add(Calendar.HOUR_OF_DAY, task.currentOccurrences.get)
            case PeriodType.Minutely => endCalendar.add(Calendar.MINUTE, task.currentOccurrences.get)
          }
        } else endCalendar.setTime(task.endDateAndTime.get)
      }
      val returnQueue: mutable.Queue[Date] = new mutable.Queue[Date]
      task.exclusions.get.foreach {
        case ExclusionDTO(_, _, Some(date), None, None, None, None, None, None) => returnQueue += date
        case ExclusionDTO(_, _, None, Some(day), None, None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(println(_))
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, None, Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), Some(year), None) =>
          returnQueue += getDateFromCalendar(day, month, year, task.timezone)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayType == dateToDayTypeString(date)) returnQueue += date

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date

        case ExclusionDTO(_, _, None, Some(day), None, None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), None, Some(criteria)) =>
          val list: List[Date] = Nil
          for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, None, Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
          val list: List[Date] = Nil
          for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), None, None, Some(month), Some(year), Some(criteria)) =>
          returnQueue += getDateFromCalendar(day, month, year, task.timezone)

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date

        case ExclusionDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayType == dateToDayTypeString(date)) returnQueue += date

        case ExclusionDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case ExclusionDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date

        case _ => println("Exclusion borked.")

      }
      Some(returnQueue.sortBy(_.getTime))
    } else None
  }

  def calculateSchedulings(task: TaskDTO): Option[scala.collection.mutable.Queue[Date]] = {
    if (task.schedulings.isDefined) {
      val startCalendar = Calendar.getInstance
      val endCalendar = Calendar.getInstance
      if (task.startDateAndTime.isDefined) startCalendar.setTime(task.startDateAndTime.get) else startCalendar.setTime(new Date())
      if (task.taskType != SchedulingType.RunOnce) {
        if (task.totalOccurrences.isDefined) {
          endCalendar.setTime(task.startDateAndTime.get)
          task.periodType.get match {
            case PeriodType.Yearly => endCalendar.add(Calendar.YEAR, task.currentOccurrences.get)
            case PeriodType.Monthly => endCalendar.add(Calendar.MONTH, task.currentOccurrences.get)
            case PeriodType.Weekly => endCalendar.add(Calendar.WEEK_OF_YEAR, task.currentOccurrences.get)
            case PeriodType.Daily => endCalendar.add(Calendar.DAY_OF_MONTH, task.currentOccurrences.get)
            case PeriodType.Hourly => endCalendar.add(Calendar.HOUR_OF_DAY, task.currentOccurrences.get)
            case PeriodType.Minutely => endCalendar.add(Calendar.MINUTE, task.currentOccurrences.get)
          }
        } else endCalendar.setTime(task.endDateAndTime.get)
      }
      val returnQueue: mutable.Queue[Date] = new mutable.Queue[Date]
      task.schedulings.get.foreach {
        case SchedulingDTO(_, _, Some(date), None, None, None, None, None, None) => returnQueue += date
        case SchedulingDTO(_, _, None, Some(day), None, None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, None, Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), Some(year), None) =>
          returnQueue :+ getDateFromCalendar(day, month, year, task.timezone)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, None) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), None) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayType == dateToDayTypeString(date)) returnQueue += date

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date)

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), None) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue += date

        case SchedulingDTO(_, _, None, Some(day), None, None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), None, Some(criteria)) =>
          val list: List[Date] = Nil
          for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, None, Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, None, Some(year), Some(criteria)) =>
          val list: List[Date] = Nil
          for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), None, None, Some(month), Some(year), Some(criteria)) =>
          returnQueue :+ getDateFromCalendar(day, month, year, task.timezone)

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, None, None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }
        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), None, Some(criteria)) =>
          val list = for {
            year <- startCalendar.get(Calendar.YEAR) to endCalendar.get(Calendar.YEAR)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), None, Some(year), Some(criteria)) =>
          val list = for {
            month <- startCalendar.get(Calendar.MONTH) to endCalendar.get(Calendar.MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), None, Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date)) returnQueue += date

        case SchedulingDTO(_, _, None, Some(day), None, Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayType == dateToDayTypeString(date)) returnQueue += date

        case SchedulingDTO(_, _, None, None, Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val list = for {
            day <- startCalendar.get(Calendar.DAY_OF_MONTH) to endCalendar.get(Calendar.DAY_OF_MONTH)
          } yield getDateFromCalendar(day, month, year, task.timezone)
          val finalList = Nil
          list.foreach(date => if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) date :: finalList)
          criteria match {
            case Criteria.First => returnQueue += list.last
            case Criteria.Second => returnQueue += list.init.last
            case Criteria.Third => returnQueue += list.init.init.last
            case Criteria.Fourth => returnQueue += list.init.init.init.last
            case Criteria.Last => returnQueue += list.head
          }

        case SchedulingDTO(_, _, None, Some(day), Some(dayOfWeek), Some(dayType), Some(month), Some(year), Some(criteria)) =>
          val date = getDateFromCalendar(day, month, year, task.timezone)
          if (dayOfWeek == dateToDayOfWeekInt(date) && dayType == dateToDayTypeString(date)) returnQueue :+ date

        case _ => println("Exclusion borked.")

      }
      Some(returnQueue.sortBy(_.getTime))
    } else None

  }

  //TODO change implementation
  def getDateFromCalendar(day: Int, month: Int, year: Int, timezone: Option[String] = None): Date = {
    val dateCalendar = Calendar.getInstance
    if (timezone.isDefined) dateCalendar.setTimeZone(parseTimezone(timezone.get).get)
    dateCalendar.set(year, month - 1, day)
    dateCalendar.getTime
  }
}
