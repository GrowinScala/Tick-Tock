package executionengine

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.actor.{Actor, ActorSystem, Props}
import api.services.SchedulingType._

import scala.concurrent.duration._


class ExecutionJob(filePath: String, schedulingType: SchedulingType, datetime: Option[Date] = None , interval: Option[FiniteDuration] = None) {

  class ExecutionActor extends Actor {

    def receive= { //TODO - if both dateTime and interval are optional, maybe we can't do this this way? (datetime.get)
      case 0 =>
        val sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        if(datetime == null) //TODO - hmmmm...
          println(getSpecificCurrentTime + " Error running file " + filePath + " scheduled at " + dateToStringFormat(datetime.get, "yyyy-MM-dd HH:mm:ss") + ".")
        else
          println(getSpecificCurrentTime + " Ran file " + filePath + " scheduled at " + dateToStringFormat(datetime.get, "yyyy-MM-dd HH:mm:ss") + ".")
      case _ => println("Program didn't run fine.")
    }
    //TODO: Error handling.
  }

  def run: Unit ={
    val delay = calculateDelay(datetime.get)
    if(delay < 0) return
    else {
      schedulingType match { //TODO - This code?
        case RunOnce =>
          val system = ActorSystem("SimpleSystem")
          val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
          implicit val ec = system.dispatcher
          system.scheduler.scheduleOnce(delay.millis)(schedulerActor ! ExecutionManager.run(filePath))
        case Periodic =>
          val system = ActorSystem("SchedulerSystem")
          val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
          implicit val ec = system.dispatcher
          system.scheduler.schedule(delay.millis, interval.get)(schedulerActor ! ExecutionManager.run(filePath))
      }
    }

  }

  def dateToStringFormat(date: Date, format: String): String ={
    val sdf = new SimpleDateFormat(format)
    sdf.format(date)
  }

  def calculateDelay(datetime: Date): Long = { //TODO - too many variables with the same name...
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
