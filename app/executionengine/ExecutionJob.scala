package executionengine

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Duration._
import java.util.{Calendar, Date}

import akka.actor.{Actor, PoisonPill, Timers}
import api.services.SchedulingType._
import api.utils.DateUtils._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionJob._
import javax.inject.Inject
import executionengine.ExecutionStatus._
import play.api.Logger

import scala.concurrent.ExecutionContext

object ExecutionJob {
  case object Start
  case object ExecuteRunOnce
  case object ExecutePeriodic
  case object ExecutePersonalized
  case object Cancel

  case object GetStatus
  case object NewStatus
}

/**
 * Class that handles independent scheduling jobs and calls the ExecutionManager
 * to run the given file with the given scheduling settings at the scheduled time(s)
 * with the use of Actors.
 * @param taskId Id of the task registered on the database.
 * @param schedulingType Type of scheduling. (One time, Periodic, etc.)
 */
class ExecutionJob @Inject() (
  taskId: String,
  fileId: String,
  schedulingType: SchedulingType,
  startDate: Option[Date] = None,
  interval: Option[Duration] = Some(ZERO),
  endDate: Option[Date] = None,
  timezone: Option[String] = None,
  var exclusions: List[Date] = Nil,
  var schedulings: List[Date] = Nil)(
  implicit
  val fileRepo: FileRepository,
  implicit val taskRepo: TaskRepository,
  implicit val executionManager: ExecutionManager) extends Actor with Timers {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private final val MAX_DELAY_SECONDS = 21474835 //max delay handled by the akka.actor.Actor system.
  private final val MAX_VARIANCE = -20000 //max variance between currentDate and endDate that accepts a date being the same as the planned one.
  val calendar: Calendar = Calendar.getInstance

  var status: ExecutionStatus = ExecutionStatus.Idle
  var latency: Long = 0
  var nextDateMillis: Long = startDate.getOrElse(getCurrentDate).getTime

  val logger: Logger = Logger(this.getClass())

  /**
   * Actor method that defined how to act when it receives a task.
   * +
   */
  def receive: Receive = {
    case Start => start()
    case ExecuteRunOnce => executeRunOnce()
    case ExecutePeriodic => executePeriodic()
    case ExecutePersonalized => executePersonalized()
    case Cancel => cancel()

    case GetStatus => sender() ! status
    case newStatus: ExecutionStatus => status = newStatus
  }

  /**
   * Method that is called when a scheduling is made. It checks for the time delay until the task is supposed to be executed
   * by subtracting the schedule time and the current time:
   * - If the task's date has already happened, no task will be scheduled and the method ends.
   * - If it exceeds the maximum delay that the scheduler can handle (which is 212575835 seconds, or ~249 days) the task is
   * delayed by scheduling with the DelayerActor so it is delayed for the maximum delay amount.
   * - If it doesn't exceed the maximum delay and its date is in the future, it is scheduled to execute the file with the
   * ExecutionActor depending on the schedulingType.
   */
  def start(): Unit = {
    val delay = calculateDelay(startDate, timezone)
    if (delay.getSeconds > MAX_DELAY_SECONDS) {
      status = ExecutionStatus.Delaying
      logger.debug("status: " + status)
      timers.startSingleTimer("delayKey", Start, Duration.ofSeconds(MAX_DELAY_SECONDS))
    } else {
      if (delay.getSeconds > 0) {
        println(schedulingType)
        schedulingType match {
          case RunOnce =>
            status = ExecutionStatus.RunOnceWaiting
            logger.debug("status: " + status)
            timers.startSingleTimer("runOnceExecutionKey", ExecuteRunOnce, delay.minusMillis(2500))
          case Periodic =>
            status = ExecutionStatus.PeriodicWaiting
            logger.debug("status: " + status)
            timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, delay)
          case Personalized =>
            status = ExecutionStatus.PersonalizedWaiting
            logger.debug("status: " + status)
            if (schedulings.nonEmpty) {
              val nextDateDelay = calculateDelay(Some(schedulings.head), timezone)
              schedulings = schedulings.tail
              timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
            }
        }
      }
    }
  }

  def executeRunOnce(): Unit = {
    executionManager.runFile(fileId)
    status = ExecutionStatus.RunOnceRunning
    printExecutionMessage()
    self ! PoisonPill
  }

  def executePeriodic(): Unit = {
    if (!isExcluded(new Date(nextDateMillis))) {
      if (endDate.isDefined) {
        if (endDate.get.getTime - getCurrentDate.getTime >= MAX_VARIANCE) {
          executionManager.runFile(fileId)
          status = ExecutionStatus.PeriodicRunning
          val currentDate = new Date()

          latency = calculateLatency(currentDate.getTime, nextDateMillis)
          nextDateMillis = calculateNextDateMillis(nextDateMillis)

          printExecutionMessage()

          timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, interval.get.minusMillis(latency))
        } else self ! PoisonPill
      } else {
        taskRepo.selectCurrentOccurrencesByTaskId(taskId).map { occurrences =>
          if (occurrences.get != 0) {
            taskRepo.decrementCurrentOccurrencesByTaskId(taskId)
            executionManager.runFile(fileId)
            status = ExecutionStatus.PeriodicRunning
            val currentDate = new Date()

            latency = calculateLatency(currentDate.getTime, nextDateMillis)
            nextDateMillis = calculateNextDateMillis(nextDateMillis)

            printExecutionMessage()
            timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, interval.get.minusMillis(latency))
          } else self ! PoisonPill
        }
      }
    }
  }

  def executePersonalized(): Unit = {
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      executionManager.runFile(fileId)
      status = ExecutionStatus.PersonalizedRunning
      printExecutionMessage()
    }
    runNextScheduling()
  }

  def printExecutionMessage(): Unit = {

    val hasStartDate = "[" + getCurrentDate + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getCurrentDate, "yyyy-MM-dd HH:mm:ss.SSS") + "."
    val noStartDate = "[" + getCurrentDate + "] Ran file " + fileId + " scheduled to run immediately."

    if (startDate.isDefined){
      logger.debug(hasStartDate)
      println(hasStartDate)
    }
    else
      logger.debug(noStartDate)
      println(noStartDate)
  }

  def printDelayMessage(): Unit = {
    val delayMessage = getCurrentDate + " Received delayed task with storageName: " + fileId
    logger.debug(delayMessage)
    println(delayMessage)
  }

  def cancel(): Unit = {
    status = ExecutionStatus.Canceled
    logger.debug("status: " + status)
    timers.cancelAll()
  }

  /**
   * Calculated the delay between the current date and time with the given date and time.
   * If the date isn't defined, it returns zero. If it's defined, it makes the calculation.
   * @param datetime Date given to calculate the delay between now and then.
   * @return Duration object holding the calculated delay.
   */
  def calculateDelay(datetime: Option[Date], timezone: Option[String]): Duration = {
    if (datetime.isEmpty) ZERO
    else {
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      if (timezone.isDefined) sdf.setTimeZone(parseTimezone(timezone.get).get)
      //val currentTime = sdf.parse(sdf.format(new Date())).getTime
      val currentTime = new Date().getTime
      val scheduledTime = sdf.parse(sdf.format(datetime.get)).getTime
      Duration.ofMillis(Math.max(scheduledTime - currentTime, 0))
    }
  }

  def isExcluded(date: Date): Boolean = {
    def iter: Boolean = {
      exclusions.headOption match {
        case Some(exclusion) =>
          exclusion.compareTo(date) match {
            case -1 =>
              exclusions.head
              exclusions = exclusions.tail
              iter
            case 0 =>
              exclusions.head
              exclusions = exclusions.tail
              true
            case 1 =>
              false
          }
        case None => false
      }
    }
    iter
  }

  /**
   * Checks if there are schedulings left to run (only applicable to Personalized tasks). If that's the case,
   * it calculates the delay for the next date and then schedules it.
   * @return
   */
  def runNextScheduling(): Unit = {
    if (schedulings.nonEmpty) {
      val nextDateDelay = calculateDelay(Some(schedulings.head), timezone)
      schedulings = schedulings.tail
      timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
    } else self ! PoisonPill
  }

  private def calculateNextDateMillis(date: Long): Long = date + interval.get.toMillis

  private def calculateLatency(currentDateMillis: Long, plannedDateMillis: Long): Long = currentDateMillis - plannedDateMillis

}
