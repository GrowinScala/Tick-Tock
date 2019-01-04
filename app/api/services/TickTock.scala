package api.services

import java.util.UUID

import akka.actor.Cancellable
import api.dtos.{FileDTO, TaskDTO}
import api.services.TaskService._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import database.mappings.FileMappings.FileRow
import database.repositories.{FileRepository, TaskRepository}
import database.utils.DatabaseUtils._
import slick.dbio

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Success

/**
  * Object that contains the main method for the project.
  */
object TickTock {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val fileRepo = new FileRepositoryImpl(DEFAULT_DB)
  val taskRepo = new TaskRepositoryImpl(DEFAULT_DB)

  val taskMap: Map[String, Cancellable] = Map[String, Cancellable]()

  def retrieveDataFromDB(implicit ec: ExecutionContext): Future[Unit] = {
//    println("retrieving data from DB")
//    taskRepo.selectAllTasks.map { seq =>
//      seq.foreach(t => fileRepo.selectNameFromFileId(t.fileId).map(name => scheduleTask(name, t.startDateAndTime)))
//    }
    ???
  }

  def main(args: Array[String]): Unit = {
//    import database.mappings.FileMappings._
//    import database.mappings.TaskMappings._
//    val tables = Seq(filesTable, tasksTable)
//
//    val db = DEFAULT_DB
//    val tableCreation = db.run(dbio.DBIO.seq(tables.map(_.schema.create): _*))
//    //fileRepo.createFilesTable
//    //taskRepo.createTasksTable
//    val result = tableCreation.onComplete{
//      case Success(_) =>
//        val res = db.run(filesTable.length.result)
//        res.map(println(_))
//      case _ =>
//    }



//  fileRepo.insertInFilesTable(FileRow(0, "EmailSender", "EmailSender", FileService.getCurrentDateTimestamp))
//  seq.foreach(t => scheduleTask(t.fileName, t.startDateAndTime))
  }

}