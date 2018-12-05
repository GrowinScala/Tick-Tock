package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import database.repositories.FileRepository._
import database.repositories.TaskRepository._
import api.services.SchedulingType._
import executionengine.ExecutionJob


object TaskService {

  def scheduleOnce(filePath: String): Unit ={
    new ExecutionJob(filePath, SchedulingType.RunOnce).run
  }

  def scheduleOnce(filePath: String, datetime: Date): Unit = {
    new ExecutionJob(filePath, SchedulingType.RunOnce, datetime).run
  }

  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime
    new Timestamp(now.getTime)
  }
}
