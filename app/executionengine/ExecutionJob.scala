package executionengine

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.actor.{Actor, ActorSystem, Props}
import api.services.SchedulingType._

import scala.concurrent.duration._


class ExecutionJob(fileName: String, schedulingType: SchedulingType, datetime: Date , interval: FiniteDuration) {

  def this(fileName: String, schedulingType: SchedulingType) = {
    this(fileName, schedulingType, null, 0 seconds)
  }

  def this(fileName: String, schedulingType: SchedulingType, datetime: Date) = {
    this(fileName, schedulingType, datetime, 0 seconds)
  }

  class ExecutionActor extends Actor {

    def receive= {
      case 0 =>
        val sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        if(datetime == null)
          println(getSpecificCurrentTime + " Error running file " + fileName + " scheduled at " + dateToStringFormat(datetime, "yyyy-MM-dd HH:mm:ss") + ".")
        else
          println(getSpecificCurrentTime + " Ran file " + fileName + " scheduled at " + dateToStringFormat(datetime, "yyyy-MM-dd HH:mm:ss") + ".")
      case _ => println("Program didn't run fine.")
    }
  }

  def run: Unit ={
    val delay = calculateDelay(datetime)
    if(delay < 0) return
    else {
      schedulingType match {
        case RunOnce =>
          val system = ActorSystem("SimpleSystem")
          val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
          implicit val ec = system.dispatcher
          system.scheduler.scheduleOnce(delay.millis)(schedulerActor ! ExecutionManager.run(fileName))
        case Periodic =>
          val system = ActorSystem("SchedulerSystem")
          val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
          implicit val ec = system.dispatcher
          system.scheduler.schedule(delay.millis, interval)(schedulerActor ! ExecutionManager.run(fileName))
      }
    }

  }

  def dateToStringFormat(date: Date, format: String): String ={
    val sdf = new SimpleDateFormat(format)
    sdf.format(date)
  }

  def calculateDelay(datetime: Date): Long = {
    if(datetime == null) 0
    else {
      val now = new Date()
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      sdf.setTimeZone(TimeZone.getTimeZone("Portugal"))
      val currentTime = sdf.parse(sdf.format(now)).getTime
      val scheduledTime = sdf.parse(sdf.format(datetime)).getTime
      scheduledTime - currentTime
    }
  }

  def getSpecificCurrentTime: String = {
    val now = new Date()
    val sdf = new SimpleDateFormat("HH:mm:ss.SSS")
    sdf.format(now)
  }

}
