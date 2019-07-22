package executionengine

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Duration._
import java.util.{ Calendar, Date }

import akka.actor.{ Actor, Timers }
import api.services.SchedulingType._
import api.utils.DateUtils._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionJob._
import javax.inject.Inject
import executionengine.ExecutionStatus._

import scala.collection._
import scala.concurrent.ExecutionContext

object ExecutionJob {
  case object Start
  case object ExecuteRunOnce
  case object ExecutePeriodic
  case object ExecutePersonalized
  case object Delay
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
  schedulings: Option[mutable.Queue[Date]] = None,
  exclusions: Option[mutable.Queue[Date]] = None)(implicit
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

  /**
   * Actor method that defined how to act when it receives a task.
   */
  def receive: Receive = {
    case Start => start()
    case ExecuteRunOnce => executeRunOnce()
    case ExecutePeriodic => executePeriodic()
    case ExecutePersonalized => executePersonalized()
    case Delay => delay()
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
    println(delay.getSeconds)
    if (delay.getSeconds > MAX_DELAY_SECONDS) {
      status = ExecutionStatus.Delaying
      timers.startSingleTimer("delayKey", Start, Duration.ofSeconds(MAX_DELAY_SECONDS))
    } else {
      if (delay.getSeconds > 0) {
        println(schedulingType)
        schedulingType match {
          case RunOnce =>
            status = ExecutionStatus.RunOnceWaiting
            timers.startSingleTimer("runOnceExecutionKey", ExecuteRunOnce, delay.minusMillis(2500))
          case Periodic =>
            status = ExecutionStatus.PeriodicWaiting
            timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, delay)
          case Personalized =>
            status = ExecutionStatus.PersonalizedWaiting
            if (schedulings.isDefined) {
              val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue), timezone)
              timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
            }
        }
      }
    }
  }

  def executeRunOnce(): Unit = {
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      executionManager.runFile(fileId)
      status = ExecutionStatus.RunOnceRunning
      printExecutionMessage()
    }
  }

  def executePeriodic(): Unit = {
    if (!isExcluded(new Date(nextDateMillis))) {
      if (endDate.isDefined) {
        if (endDate.get.getTime - getCurrentDate.getTime >= MAX_VARIANCE) {
          executionManager.runFile(fileId)
          status = ExecutionStatus.PeriodicRunning
          val currentDate = new Date()
          println("current date: " + dateToStringFormat(currentDate, "yyyy-MM-dd HH:mm:ss.SSS"))
          println("planned date: " + dateToStringFormat(new Date(nextDateMillis), "yyyy-MM-dd HH:mm:ss.SSS"))
          latency = calculateLatency(currentDate.getTime, nextDateMillis)
          nextDateMillis = calculateNextDateMillis(nextDateMillis)
          println("calculated latency: " + latency)
          println("next date: " + dateToStringFormat(new Date(nextDateMillis), "yyyy-MM-dd HH:mm:ss.SSS"))
          println("-------------------")
          println("interval: " + interval.get.getSeconds)
          println("latency: " + latency)
          printExecutionMessage()
          timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, interval.get.minusMillis(latency))
        } else status = ExecutionStatus.Idle
      } else {
        taskRepo.selectCurrentOccurrencesByTaskId(taskId).map { occurrences =>
          if (occurrences.get != 0) {
            taskRepo.decrementCurrentOccurrencesByTaskId(taskId)
            executionManager.runFile(fileId)
            status = ExecutionStatus.PeriodicRunning
            val currentDate = new Date()
            println("current date: " + dateToStringFormat(currentDate, "yyyy-MM-dd HH:mm:ss.SSS"))
            println("planned date: " + dateToStringFormat(new Date(nextDateMillis), "yyyy-MM-dd HH:mm:ss.SSS"))
            latency = calculateLatency(currentDate.getTime, nextDateMillis)
            nextDateMillis = calculateNextDateMillis(nextDateMillis)
            println("calculated latency: " + latency)
            println("next date: " + dateToStringFormat(new Date(nextDateMillis), "yyyy-MM-dd HH:mm:ss.SSS"))
            printExecutionMessage()
            timers.startSingleTimer("periodicExecutionKey", ExecutePeriodic, interval.get.minusMillis(latency))
          } else status = ExecutionStatus.Idle
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
    if (startDate.isDefined) println("[" + getCurrentDate + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getCurrentDate, "yyyy-MM-dd HH:mm:ss.SSS") + ".")
    else println("[" + getCurrentDate + "] Ran file " + fileId + " scheduled to run immediately.")
  }

  def delay(): Unit = executionManager.delayFile(ExecutionJob.this)

  def printDelayMessage(): Unit = println(getCurrentDate + " Received delayed task with storageName: " + fileId)

  def cancel(): Unit = status = ExecutionStatus.Canceled; timers.cancelAll()

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
      exclusions match {
        case Some(exclusionQueue) =>
          exclusionQueue.headOption match {
            case Some(exclusion) =>
              exclusion.compareTo(date) match {
                case -1 =>
                  exclusionQueue.dequeue()
                  iter
                case 0 =>
                  exclusionQueue.dequeue()
                  true
                case 1 =>
                  false
              }
            case None => false
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
    if (schedulings.isDefined && schedulings.get.nonEmpty) {
      val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue), timezone)
      timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
    } else status = ExecutionStatus.Idle
  }

  private def calculateNextDateMillis(date: Long): Long = date + interval.get.toMillis

  private def calculateLatency(currentDateMillis: Long, plannedDateMillis: Long): Long = currentDateMillis - plannedDateMillis

}
