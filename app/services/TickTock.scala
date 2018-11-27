package services

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import slick.driver.MySQLDriver.api._
import DBConnector._
import services.ExecutionManager.storagePath

import scala.sys.process._

object TickTock{

  object SchedulingType extends Enumeration {
    type SchedulingType = Value
    val RunOnce, Periodic = Value
  }

  def scheduleOnce(fileName: String): Unit ={
    new ScheduleJob(fileName, SchedulingType.RunOnce).run
  }

  def scheduleOnce(fileName: String, datetime: Date): Unit = {
    //val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //val date: Date = format.parse(datetime)
    new ScheduleJob(fileName, SchedulingType.RunOnce, datetime).run
  }

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    exec(selectAllFromTasksTable.result).foreach(t => scheduleOnce(selectNameFromFileId(t.fileId).head, t.startDateAndTime))
  }

  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime()
    new Timestamp(now.getTime)
    //val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //format.format(now)
  }

  def main(args: Array[String]): Unit = {

    /*val flyway: Flyway = Flyway.configure().dataSource(
      "jdbc:mysql://127.0.0.1:3306/ticktock?serverTimezone=Portugal",
      "root",
      "growin"
    ).load()

    flyway.baseline()
    flyway.migrate()*/

  }

}