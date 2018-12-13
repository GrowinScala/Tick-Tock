package api.services

import java.sql.Timestamp
import java.util.{Calendar, Date}

import executionengine.ExecutionJob

/**
  * Object that contains all methods for the task scheduling related to the service layer.
  */
object TaskService {

  /**
    * Schedules a task by giving the storageName to be executed immediately.
    * @param storageName Name of the file on the storage folder.
    */
  def scheduleTask(storageName: String): Unit ={
    new ExecutionJob(storageName, SchedulingType.RunOnce).start
  }

  /**
    * Schedules a task by giving the storageName to be executed on the given datetime.
    * @param storageName Name of the file on the storage folder.
    * @param datetime Date specifying when the task is executed.
    */
  def scheduleTask(storageName: String, datetime: Date): Unit = {
    new ExecutionJob(storageName, SchedulingType.RunOnce, Some(datetime)).start
  }

  /**
    * Auxiliary method that returns the current date in the Timestamp format.
    */
  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime
    new Timestamp(now.getTime)
  }
}
