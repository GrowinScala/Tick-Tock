package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import database.repositories.FileRepository._
import database.repositories.TaskRepository._
import api.services.SchedulingType._
import executionengine.ExecutionJob


object TaskService {

  def scheduleOnce(fileName: String): Unit ={
    new ExecutionJob(fileName, SchedulingType.RunOnce)
  }

  def scheduleOnce(fileName: String, datetime: Date): Unit = {
    //val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //val date: Date = format.parse(datetime)
    new ExecutionJob(fileName, SchedulingType.RunOnce, datetime).run
  }

  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime()
    new Timestamp(now.getTime)
    //val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //format.format(now)
  }
}
