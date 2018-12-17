package api.services

import api.services.TaskService._
import database.repositories.{FileRepository, TaskRepository}
import slick.jdbc.MySQLProfile.api._
import database.utils.DatabaseUtils._

/**
  * Object that contains the main method for the project.
  */
object TickTock{

  val fileRepo = new FileRepository(DEFAULT_DB)
  val taskRepo = new TaskRepository(DEFAULT_DB)

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    taskRepo.selectAllTasks.foreach(t => scheduleTask(fileRepo.selectFileNameFromFileId(t.fileId), t.startDateAndTime))
  }

  def main(args: Array[String]): Unit = {
    retrieveDataFromDB
  }

}