package database.repositories

import api.dtos.TaskDTO
import database.mappings.TaskMappings.TaskRow
import slick.jdbc.MySQLProfile.api._
import database.mappings.TaskMappings._
import slick.dbio.DBIO

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Class that handles the data layer for the scheduled tasks.
  * It contains task scheduling related queries to communicate with the database.
  * @param db Database class that contains the database information.
  */
class TaskRepository(db: Database){

  val fileRepo = new FileRepository(db)

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  /**
    * Selects all tasks from the tasks table on the database.
    * @return
    */
  def selectAllTasks: Seq[TaskRow] = {
    exec(selectAllFromTasksTable.result)
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
    * @param task TaskRow to be inserted.
    */
  def insertInTasksTable(task: TaskRow): Unit = {
    if(fileRepo.existsCorrespondingFileId(task.fileId)) exec(insertTask(task))
    else println("Could not insert Task with id " + task.fileId + " due to not finding a corresponding File.")
  }

  /**
    * Inserts a task (row) on the tasks table on the database.
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Unit = {
    if(fileRepo.existsCorrespondingFileName(task.taskName)) exec(insertTask(TaskRow(0, fileRepo.selectFileIdFromName(task.taskName), task.startDateAndTime)))
    else println("Could not insert Task with name " + task.taskName + "due to not finding a corresponding File.")
  }
}
