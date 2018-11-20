package services

import java.text.SimpleDateFormat
import java.util.{Date, Calendar}

import slick.driver.MySQLDriver.api._

import DBConnector._

object TickTock{

  object SchedulingType extends Enumeration {
    type SchedulingType = Value
    val RunOnce, Periodic = Value
  }

  def scheduleOnce(fileName: String): Unit ={
    new ScheduleJob(fileName, SchedulingType.RunOnce).run
  }

  def scheduleOnce(fileName: String, datetime: String): Unit = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date: Date = format.parse(datetime)
    new ScheduleJob(fileName, SchedulingType.RunOnce, date).run
  }

  def retrieveDataFromDB = {
    exec(selectAllFromTasksTable.result).foreach(t => scheduleOnce(t.fileName, t.startDateAndTime))
  }

  def getCurrentDateTimeString: String = {
    val now = Calendar.getInstance().getTime()
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.format(now)
  }

  def main(args: Array[String]): Unit = {

    /*val flyway: Flyway = Flyway.configure().dataSource(
      "jdbc:mysql://127.0.0.1:3306/ticktock?serverTimezone=Portugal",
      "root",
      "growin"
    ).load()

    flyway.baseline()
    flyway.migrate()*/

    retrieveDataFromDB

  }

}
