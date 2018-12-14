package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import database.repositories.FileRepository._
import database.repositories.TaskRepository._
import api.services.SchedulingType._
import executionengine.ExecutionJob


object TaskService {

  def scheduleOnce(storageName: String): Unit ={
    new ExecutionJob(storageName, SchedulingType.RunOnce).run
  }

  def scheduleOnce(storageName: String, datetime: Date): Unit = {
    new ExecutionJob(storageName, SchedulingType.RunOnce, Some(datetime)).run
  }

  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime
    new Timestamp(now.getTime)
  }
}
