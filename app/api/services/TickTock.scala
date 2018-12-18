package api.services

import api.dtos.TaskDTO
import api.services.TaskService._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import database.utils.DatabaseUtils._
import api.utils.DateUtils._

/**
  * Object that contains the main method for the project.
  */
object TickTock{

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepositoryImpl(db)
  val taskRepo = new TaskRepositoryImpl(db)

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.foreach(t => scheduleTask(t.fileName, t.startDateAndTime))
  }

  def main(args: Array[String]): Unit = {
    taskRepo.insertInTasksTable(TaskDTO(stringToDateFormat("2018-12-18 11:30:00", "yyyy-MM-dd HH:mm:ss"), "hello"))
  }

}