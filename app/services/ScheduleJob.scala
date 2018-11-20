package services

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import scala.concurrent.duration._
import akka.actor.{Actor, ActorSystem, Props}
import services.TickTock.SchedulingType._


class ScheduleJob(fileName: String, schedulingType: SchedulingType, datetime: Date, interval: FiniteDuration) extends App {

  def this(fileName: String, schedulingType: SchedulingType){
    this(fileName, schedulingType, null, 0 seconds)
  }

  def this(fileName: String, schedulingType: SchedulingType, datetime: Date){
    this(fileName, schedulingType, datetime, 0 seconds)
  }

  class ExecutionActor extends Actor{

    def receive= {
      case 0 =>
        val sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        if(datetime == null) println("Program ran fine.")
        else println("Program ran fine. Date: " + sdf.format(datetime))
      case _ => println("Program didn't run fine.")
    }
  }

  def run: Unit ={
    schedulingType match {
      case RunOnce =>
        val system = ActorSystem("SimpleSystem")
        val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
        implicit val ec = system.dispatcher
        system.scheduler.scheduleOnce(calculateDelay(datetime).millis)(schedulerActor ! ExecutionManager.run(fileName))
      case Periodic =>
        val system = ActorSystem("SchedulerSystem")
        val schedulerActor = system.actorOf(Props(new ExecutionActor), "Actor")
        implicit val ec = system.dispatcher
        system.scheduler.schedule(calculateDelay(datetime).millis, interval)(schedulerActor ! ExecutionManager.run(fileName))
    }

  }

  def getCurrentDateTimeMillis: Long = ???

  def calculateDelay(datetime: Date): Long = {
    if(datetime == null) 0
    else {
      val now = new Date()
      val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      sdf.setTimeZone(TimeZone.getTimeZone("Portugal"))
      val currentTime = sdf.parse(sdf.format(now)).getTime
      println(currentTime)
      val scheduledTime = sdf.parse(sdf.format(datetime)).getTime
      println(scheduledTime)
      scheduledTime - currentTime
    }
  }

}
