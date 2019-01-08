package api.services

import api.dtos.{FileDTO, TaskDTO}
import api.services.TaskService._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import database.mappings.FileMappings.FileRow
import database.repositories.{FileRepository, TaskRepository}
import database.utils.DatabaseUtils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Object that contains the main method for the project.
  */
object TickTock {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepositoryImpl(db)
  val taskRepo = new TaskRepositoryImpl(db)

  def retrieveDataFromDB(implicit ec: ExecutionContext): Future[Unit] = {
//    println("retrieving data from DB")
//    taskRepo.selectAllTasks.map { seq =>
//      seq.foreach(t => fileRepo.selectNameFromFileId(t.fileId).map(name => scheduleTask(name, t.startDateAndTime)))
//    }
    ???
  }

  def main(args: Array[String]): Unit = {
//    fileRepo.dropFilesTable
//    taskRepo.dropTasksTable

    fileRepo.createFilesTable
    taskRepo.createTasksTable

//    retrieveDataFromDB
//      seq.foreach(t => scheduleTask(t.fileName, t.startDateAndTime))
  }

}