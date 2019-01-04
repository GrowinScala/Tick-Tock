package executionengine

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.actor.{Actor, ActorSystem, Props}
import api.services.SchedulingType._
import java.time.Duration
import java.time.Duration._

import api.services.SchedulingType
import api.utils.DateUtils._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import database.utils.DatabaseUtils._
import api.services.TickTock._

import scala.concurrent.ExecutionContext


/**
  * Class that handles independent scheduling jobs and calls the ExecutionManager
  * to run the given file with the given scheduling settings at the scheduled time(s)
  * with the use of Actors.
  * @param taskId Id of the task registered on the database.
  * @param schedulingType Type of scheduling. (One time, Periodic, etc.)
  * @param datetime Date of when the file is run. (If Periodic, it represents the first execution)
  * @param interval Time interval between each execution. (Only applicable in a Periodic task)
  */
class ExecutionJob(taskId: String, fileId: String, schedulingType: SchedulingType, startDate: Option[Date] = None, interval: Option[Duration] = Some(ZERO), endDate: Option[Date] = None, occurrences: Option[Int] = None) {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(DEFAULT_DB)

  val hasOccurrences = occurrences.isDefined
  val hasEndDate = endDate.isDefined

  /**
    * Actor and Runnable that handles file executions.
    */
  object ExecutionActor extends Actor with Runnable{

    /**
      * Inherited method from the Runnable trait that is used when the scheduling job fires.
      * It calls the runFile method from the ExecutionManager object so it executes the file asynchronously.
      */
    def run(): Unit = {
      if(hasEndDate){
        if(endDate.get.before(getCurrentDate)){
          taskMap.get(taskId).get.cancel
        }
        else{
          schedulerActor ! ExecutionManager.runFile(fileId)
        }
      }
      else {
        if(hasOccurrences){
          if(occurrences.get == 0){
            taskMap.get(taskId).get.cancel
          }
          else{
            schedulerActor ! ExecutionManager.runFile(fileId)
          }
        }
        else{
          schedulerActor ! ExecutionManager.runFile(taskId)
        }
      }
    }

    /**
      * Inherited method from the Actor trait that executes after a scheduled task finishes its execution.
      * It handles the received error code from the executed file and prints out the result.
      */
    def receive= { //TODO - if both dateTime and interval are optional, maybe we can't do this this way? (datetime.get)
      case 0 =>
        if(startDate.isEmpty)
          println(getSpecificCurrentTime + " Error running file " + fileId + " scheduled at " + dateToStringFormat(startDate.get, "yyyy-MM-dd HH:mm:ss") + ".")
        else
          println(getSpecificCurrentTime + " Ran file " + fileId + " scheduled at " + dateToStringFormat(startDate.get, "yyyy-MM-dd HH:mm:ss") + ".")
      case _ => println("Failed to run file: " + fileId)
    }
    //TODO: Error handling / handle Option[Date] better when its None.
  }

  /**
    * Actor and Runnable that handles task scheduling when the scheduled task happens.
    * in over 21474835 seconds (~249 days) in the future, which is the max time delay for a task to happen
    * for the akka-scheduler.
    */
  object DelayerActor extends Actor with Runnable{

    /**
      * Inherited method from the Runnable trait that is used when the scheduling job fires.
      * It calls the delayFile method from the ExecutionManager object so it relaunches another ExecutionJob
      * after the max delay ends for the current ExecutionJob
      */
    def run(): Unit = {
      delayerActor ! ExecutionManager.delayFile(ExecutionJob.this)
    }

    /**
      * Inherited method from the Actor trait that executes after a scheduled task is delayed.
      * It prints out the current time and storageName for the file that was delayed.
      */
    def receive = {
      case _ => println(getSpecificCurrentTime + " Received delayed task with storageName: " + fileId)
    }
  }

  private final val MAX_DELAY_SECONDS = 21474835 //max delay handled by the akka.actor.Actor system.

  val system = ActorSystem("SchedulerSystem")
  val schedulerActor = system.actorOf(Props(ExecutionActor), "SchedulerActor")
  val delayerActor = system.actorOf(Props(DelayerActor), "DelayerActor")
  implicit val sd = system.dispatcher

  /**
    * Method that is called when a scheduling is made. It checks for the time delay until the task is supposed to be executed
    * by subtracting the schedule time and the current time:
    * - If the task's date has already happened, no task will be scheduled and the method ends.
    * - If it exceeds the maximum delay that the scheduler can handle (which is 212575835 seconds, or ~249 days) the task is
    * delayed by scheduling with the DelayerActor so it is delayed for the maximum delay amount.
    * - If it doesn't exceed the maximum delay and its date is in the future, it is scheduled to execute the file with the
    * ExecutionActor depending on the schedulingType.
    */
  def start: Unit ={
    val delay = calculateDelay(startDate)
    if(delay.toMillis < 0) return
    else if(delay.getSeconds > MAX_DELAY_SECONDS) system.scheduler.scheduleOnce(Duration.ofSeconds(MAX_DELAY_SECONDS), DelayerActor)
    else {
      schedulingType match {
        case RunOnce =>
          val cancellable = system.scheduler.scheduleOnce(delay, ExecutionActor)
          taskMap + (taskId -> cancellable)
        case Periodic =>
          val cancellable = system.scheduler.schedule(delay, interval.get, ExecutionActor)
          taskMap + (taskId -> cancellable)
      }
    }
  }

  /**
    * Calculated the delay between the current date and time with the given date and time.
    * If the date isn't defined, it returns zero. If it's defined, it makes the calculation.
    * @param datetime Date given to calculate the delay between now and then.
    * @return Duration object holding the calculated delay.
    */
  def calculateDelay(datetime: Option[Date]): Duration = {
    if(datetime.isEmpty) ZERO
    else {
      val now = new Date()
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      sdf.setTimeZone(TimeZone.getTimeZone("Portugal"))
      val currentTime = sdf.parse(sdf.format(now)).getTime
      val scheduledTime = sdf.parse(sdf.format(datetime.get)).getTime
      Duration.ofMillis(scheduledTime - currentTime)
    }
  }

}
