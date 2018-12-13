package api.services

import api.services.TaskService._
import database.repositories.{FileRepository, TaskRepository}
import slick.jdbc.MySQLProfile.api._

/**
  * Object that contains the main method for the project.
  */
object TickTock{

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepository(db)
  val taskRepo = new TaskRepository(db)

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.foreach(t => scheduleTask(fileRepo.selectFileNameFromFileId(t.fileId), t.startDateAndTime))
  }

  def main(args: Array[String]): Unit = {
    retrieveDataFromDB
  }

}