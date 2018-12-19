package database.repositories.slick

import api.dtos.TaskDTO
import database.mappings.TaskMappings.{TaskRow, _}
import database.repositories.TaskRepository
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import database.repositories.FileRepository

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  * @param db Database class that contains the database information.
  */
class TaskRepositoryImpl(dtbase: Database) extends TaskRepository{

  val fileRepo = new FileRepositoryImpl(dtbase)

  def exec[T](action: DBIO[T]): T = Await.result(dtbase.run(action), 2 seconds)

  /**
    * Selects all tasks from the tasks table on the database.
    * @return
    */
  def selectAllTasks: Seq[TaskDTO] = {
    val row = exec(selectAllFromTasksTable.result)
    row.map(elem => TaskDTO(elem.startDateAndTime, fileRepo.selectFileNameFromFileId(elem.fileId)))
  }

  /**
    * Deletes all tasks from the tasks table on the database.
    */
  def deleteAllTasks: Unit = {
    exec(deleteAllFromTasksTable)
  }

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Unit = {
    exec(createTasksTableAction)
  }

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Unit = {
    exec(dropTasksTableAction)
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Unit = {
    exec(insertTask(TaskRow(0, fileRepo.selectFileIdFromName(task.fileName), task.startDateAndTime)))

  }
}
