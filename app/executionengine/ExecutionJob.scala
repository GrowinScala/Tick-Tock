package executionengine

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Duration._
import java.util.{ Calendar, Date }

import akka.actor.{ Actor, Timers }
import api.services.SchedulingType
import api.services.SchedulingType._
import api.utils.DateUtils._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionJob._
import javax.inject.Inject

import scala.collection._
import scala.concurrent.ExecutionContext

object ExecutionJob {
  case object Start
  case object ExecuteRunOnce
  case object ExecutePeriodic
  case object ExecutePersonalized
  case object Delay
  case object Cancel
}

/**
 * Class that handles independent scheduling jobs and calls the ExecutionManager
 * to run the given file with the given scheduling settings at the scheduled time(s)
 * with the use of Actors.
 * @param taskId Id of the task registered on the database.
 * @param schedulingType Type of scheduling. (One time, Periodic, etc.)
 */
class ExecutionJob @Inject() (taskId: String, fileId: String, schedulingType: SchedulingType, startDate: Option[Date] = None, interval: Option[Duration] = Some(ZERO), endDate: Option[Date] = None, timezone: Option[String] = None, schedulings: Option[mutable.Queue[Date]] = None, exclusions: Option[mutable.Queue[Date]] = None)(implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val executionManager: ExecutionManager) extends Actor with Timers {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val calendar: Calendar = Calendar.getInstance

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
  }

  private final val MAX_DELAY_SECONDS = 21474835 //max delay handled by the akka.actor.Actor system.

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
    val delay = calculateDelay(startDate)
    if (delay.getSeconds > MAX_DELAY_SECONDS && delay.toMillis >= 0) {
      timers.startSingleTimer("delayKey", Delay, Duration.ofSeconds(MAX_DELAY_SECONDS))
    } else {
      schedulingType match {
        case RunOnce =>
          timers.startSingleTimer("runOnceExecutionKey", ExecuteRunOnce, delay)
        case Periodic =>
          timers.startPeriodicTimer("periodicExecutionKey", ExecutePeriodic, interval.get)
        case Personalized =>
          val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue))
          timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
      }
    }
  }

  def executeRunOnce(): Unit = {
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      executionManager.runFile(fileId)
      printExecutionMessage()
    }
  }

  def executePeriodic(): Unit = {
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      if (endDate.isDefined) {
        if (getCurrentDate.after(endDate.get)) {
          executionManager.runFile(fileId)
          printExecutionMessage()
        }
      } else {
        taskRepo.selectCurrentOccurrencesByTaskId(taskId).map { occurrences =>
          if (occurrences.get != 0) {
            taskRepo.decrementCurrentOccurrencesByTaskId(taskId)
            executionManager.runFile(fileId)
            printExecutionMessage()
          }
        }
      }
    }
  }

  def executePersonalized(): Unit = {
    if (schedulings.isDefined && schedulings.get.nonEmpty) {
      val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue))
      timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
    }
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      executionManager.runFile(fileId)
      printExecutionMessage()
    }
  }

  /*def execute(): Unit = {
    val currentDate = getTimeFromDate(new Date())
    if (!isExcluded(currentDate)) {
      if (schedulingType == SchedulingType.RunOnce) {
        executionManager.runFile(fileId)
        printExecutionMessage()
      } else { //if it's not a RunOnce task, its a periodic/personalized one, so it has to have either endDate or occurrences
        if (endDate.isDefined) {
          if (getCurrentDate.after(endDate.get)) {
            executionManager.runFile(fileId)
            printExecutionMessage()
            if (schedulings.isDefined && schedulings.get.nonEmpty) {
              val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue))
              timers.startSingleTimer("executionKey", Execute, nextDateDelay)
            }
          }
        } else { //if it doesn't have endDate, it has occurrences.
          taskRepo.selectCurrentOccurrencesByTaskId(taskId).map { occurrences =>
            if (occurrences.get != 0) {
              taskRepo.decrementCurrentOccurrencesByTaskId(taskId)
              executionManager.runFile(fileId); printExecutionMessage()
              if (schedulings.isDefined && schedulings.get.nonEmpty) {
                val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue))
                timers.startSingleTimer("executionKey", Execute, nextDateDelay)
              }
            }
          }
        }
      }
    }
  }*/

  def printExecutionMessage(): Unit = {
    if (startDate.isDefined) println("Ran file " + fileId + " scheduled to run at " + dateToStringFormat(startDate.get, "yyyy-MM-dd HH:mm:ss") + ".")
    else println("Ran file " + fileId + " scheduled to run immediately.")
  }

  def delay(): Unit = executionManager.delayFile(ExecutionJob.this)

  def printDelayMessage(): Unit = println(getSpecificCurrentTime + " Received delayed task with storageName: " + fileId)

  def cancel(): Unit = timers.cancelAll()

  /**
   * Calculated the delay between the current date and time with the given date and time.
   * If the date isn't defined, it returns zero. If it's defined, it makes the calculation.
   * @param datetime Date given to calculate the delay between now and then.
   * @return Duration object holding the calculated delay.
   */
  def calculateDelay(datetime: Option[Date]): Duration = {
    if (datetime.isEmpty) ZERO
    else {
      val now = new Date()
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val currentTime = sdf.parse(sdf.format(now)).getTime
      val scheduledTime = sdf.parse(sdf.format(datetime.get)).getTime
      Duration.ofMillis(scheduledTime - currentTime)
    }
  }

  def isExcluded(date: Date): Boolean = {
    def iter: Boolean = {
      val exclusion = exclusions.get.dequeue
      if (exclusion.before(date)) iter
      else exclusion.equals(date)
    }
    if (exclusions.isDefined && exclusions.get.nonEmpty) iter
    else false

  }

  /**
   * Checks if there are schedulings left to run (only applicable to Personalized tasks). If that's the case,
   * it calculates the delay for the next date and then schedules it.
   * @return
   */
  def runNextScheduling(): Unit = {
    if (schedulings.isDefined && schedulings.get.nonEmpty) {
      val nextDateDelay = calculateDelay(Some(schedulings.get.dequeue))
      timers.startSingleTimer("personalizedExecutionKey", ExecutePersonalized, nextDateDelay)
    }
  }

}
